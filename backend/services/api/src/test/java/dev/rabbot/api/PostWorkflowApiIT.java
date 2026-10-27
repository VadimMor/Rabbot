package dev.rabbot.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Жизненный цикл поста: переходы статусов, ревью, версии, изоляция.
class PostWorkflowApiIT extends AbstractIntegrationTest {

    private UUID tenantA;
    private UUID tenantB;
    private String brandOfA;

    @BeforeEach
    void setUp() throws Exception {
        tenantA = createTenant("tenant-a");
        tenantB = createTenant("tenant-b");
        brandOfA = createBrand(tenantA, """
                {"name": "Rabbot Coffee"}""");
    }

    // ---------- помощники ----------

    // Пост с одним выбранным вариантом — его можно отправить на ревью.
    private String createReadyPost() throws Exception {
        return createPost("""
                [{"platform": "TELEGRAM", "body": "Готовый текст"}]""");
    }

    private String createPost(String variantsJson) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\": \"Пост\", \"kind\": \"TEXT\", \"variants\": " + variantsJson + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(body(result), "$.id");
    }

    // POST /posts/{id}/{action}?version=N без тела.
    private ResultActions action(String postId, String action, int version) throws Exception {
        return mvc.perform(post("/api/v1/posts/{postId}/{action}", postId, action)
                .param("version", String.valueOf(version))
                .header(TENANT_HEADER, tenantA));
    }

    // То же, но с JSON-телом (комментарий).
    private ResultActions action(String postId, String action, int version, String json) throws Exception {
        return mvc.perform(post("/api/v1/posts/{postId}/{action}", postId, action)
                .param("version", String.valueOf(version))
                .header(TENANT_HEADER, tenantA)
                .contentType(APPLICATION_JSON)
                .content(json));
    }

    private String statusInDb(String postId) {
        return jdbc.queryForObject("SELECT status FROM core.post WHERE id = ?::uuid", String.class, postId);
    }

    private void setStatus(String postId, String status) {
        jdbc.update("UPDATE core.post SET status = ? WHERE id = ?::uuid", status, postId);
    }

    // ---------- основной путь ----------

    @Test
    void fullReviewCycle() throws Exception {
        String id = createReadyPost();

        action(id, "submit", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.version").value(1));

        action(id, "request-changes", 1, """
                {"comment": "Добавьте призыв к действию"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.version").value(2));

        action(id, "submit", 2)
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.version").value(3));

        action(id, "approve", 3, """
                {"comment": "Отлично"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.approvedAt", notNullValue()));

        // История: новые решения первыми, у каждой записи есть время из БД.
        mvc.perform(get("/api/v1/posts/{postId}/reviews", id).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].decision").value("APPROVED"))
                .andExpect(jsonPath("$[0].comment").value("Отлично"))
                .andExpect(jsonPath("$[1].decision").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$[1].comment").value("Добавьте призыв к действию"))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()))
                // Авторизации пока нет — ревьюер не известен.
                .andExpect(jsonPath("$[0].reviewerId", nullValue()));
    }

    @Test
    void approveWithoutBodyIsAllowed() throws Exception {
        String id = createReadyPost();
        action(id, "submit", 0).andExpect(status().isOk());

        // Тело необязательно: комментарий при одобрении не нужен.
        action(id, "approve", 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        String comment = jdbc.queryForObject(
                "SELECT comment FROM core.review WHERE post_id = ?::uuid", String.class, id);
        assertThat(comment).isNull();
    }

    // Регрессия: сохранение Review падало с UnsupportedLockAttemptException,
    // когда сущность была @Immutable, а created_at — @Generated.
    @Test
    void reviewIsStoredWithDatabaseTimestamp() throws Exception {
        String id = createReadyPost();
        action(id, "submit", 0).andExpect(status().isOk());
        action(id, "request-changes", 1, """
                {"comment": "Короче"}""").andExpect(status().isOk());

        Integer rows = jdbc.queryForObject("""
                SELECT count(*) FROM core.review
                WHERE post_id = ?::uuid AND decision = 'CHANGES_REQUESTED' AND created_at IS NOT NULL""",
                Integer.class, id);
        assertThat(rows).isEqualTo(1);
    }

    // ---------- условия отправки на ревью ----------

    @Test
    void submitRequiresSelectedVariantWithText() throws Exception {
        // Без вариантов.
        String empty = createPost("[]");
        action(empty, "submit", 0).andExpect(status().isConflict());

        // Вариант есть, но текст пустой.
        String blank = createPost("""
                [{"platform": "TELEGRAM", "body": "   "}]""");
        action(blank, "submit", 0).andExpect(status().isConflict());

        assertThat(statusInDb(empty)).isEqualTo("DRAFT");
        assertThat(statusInDb(blank)).isEqualTo("DRAFT");
    }

    @Test
    void requestChangesRequiresComment() throws Exception {
        String id = createReadyPost();
        action(id, "submit", 0).andExpect(status().isOk());

        action(id, "request-changes", 1, """
                {"comment": "  "}""").andExpect(status().isBadRequest());
        action(id, "request-changes", 1, "{}").andExpect(status().isBadRequest());

        assertThat(statusInDb(id)).isEqualTo("IN_REVIEW");
        Integer reviews = jdbc.queryForObject("SELECT count(*) FROM core.review", Integer.class);
        assertThat(reviews).isZero();
    }

    // ---------- запрещённые переходы ----------

    @Test
    void forbiddenTransitionsReturn409() throws Exception {
        String id = createReadyPost();

        // Черновик нельзя одобрить или вернуть на доработку — он ещё не на ревью.
        action(id, "approve", 0).andExpect(status().isConflict());
        action(id, "request-changes", 0, """
                {"comment": "x"}""").andExpect(status().isConflict());

        // Одобренный нельзя снова отправить на ревью.
        setStatus(id, "APPROVED");
        action(id, "submit", 0).andExpect(status().isConflict());

        // В расписании — только через отмену публикации (будет в scheduler).
        setStatus(id, "SCHEDULED");
        action(id, "archive", 0).andExpect(status().isConflict());

        // Архив — конечный статус.
        setStatus(id, "ARCHIVED");
        action(id, "archive", 0).andExpect(status().isConflict());
        action(id, "submit", 0).andExpect(status().isConflict());

        // Ни одна попытка не записала ревью.
        Integer reviews = jdbc.queryForObject("SELECT count(*) FROM core.review", Integer.class);
        assertThat(reviews).isZero();
    }

    @Test
    void archiveIsAllowedFromDraftAndApproved() throws Exception {
        String draft = createReadyPost();
        action(draft, "archive", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        String approved = createReadyPost();
        action(approved, "submit", 0).andExpect(status().isOk());
        action(approved, "approve", 1).andExpect(status().isOk());
        action(approved, "archive", 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void approvedPostCannotBeEdited() throws Exception {
        String id = createReadyPost();
        action(id, "submit", 0).andExpect(status().isOk());
        action(id, "approve", 1).andExpect(status().isOk());

        // После одобрения текст заморожен.
        mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Поздняя правка", "version": 2}"""))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/posts/{postId}/variants", id)
                        .param("version", "2")
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"platform": "VK", "body": "Ещё вариант"}"""))
                .andExpect(status().isConflict());
    }

    // ---------- версия ----------

    @Test
    void staleVersionReturns409() throws Exception {
        String id = createReadyPost();
        action(id, "submit", 0).andExpect(status().isOk());

        // Ревьюер открыл пост на версии 0 и пытается одобрить.
        action(id, "approve", 0).andExpect(status().isConflict());

        assertThat(statusInDb(id)).isEqualTo("IN_REVIEW");
    }

    @Test
    void missingVersionReturns400() throws Exception {
        String id = createReadyPost();

        mvc.perform(post("/api/v1/posts/{postId}/submit", id).header(TENANT_HEADER, tenantA))
                .andExpect(status().isBadRequest());
    }

    // ---------- чужие данные и связи ----------

    @Test
    void otherTenantGets404() throws Exception {
        String id = createReadyPost();

        mvc.perform(post("/api/v1/posts/{postId}/submit", id)
                        .param("version", "0")
                        .header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/posts/{postId}/reviews", id).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        assertThat(statusInDb(id)).isEqualTo("DRAFT");
    }

    @Test
    void deletingBrandDeletesReviews() throws Exception {
        String id = createReadyPost();
        action(id, "submit", 0).andExpect(status().isOk());
        action(id, "approve", 1).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/brands/{id}", brandOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isNoContent());

        // brand → post → review: каскад через внешние ключи.
        Integer reviews = jdbc.queryForObject("SELECT count(*) FROM core.review", Integer.class);
        assertThat(reviews).isZero();
    }
}