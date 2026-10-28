package dev.rabbot.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// История текста вариантов (post_revision).
class PostRevisionApiIT extends AbstractIntegrationTest {

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

    // Создать пост и вернуть JSON ответа.
    private String createPost(String variantsJson) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\": \"Пост\", \"kind\": \"TEXT\", \"variants\": " + variantsJson + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result);
    }

    private String createSimplePost(String text) throws Exception {
        return createPost("[{\"platform\": \"TELEGRAM\", \"body\": \"" + text + "\"}]");
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }

    private static int version(String json) {
        return JsonPath.read(json, "$.version");
    }

    private static String firstVariant(String json) {
        return JsonPath.read(json, "$.variants[0].id");
    }

    // Изменить текст и вернуть новый JSON поста.
    private String edit(String postJson, String variantId, String bodyJson) throws Exception {
        MvcResult result = mvc.perform(put("/api/v1/posts/{postId}/variants/{variantId}", id(postJson), variantId)
                        .param("version", String.valueOf(version(postJson)))
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andReturn();
        return body(result);
    }

    private ResultActions revisions(UUID tenant, String postId, String variantId) throws Exception {
        return mvc.perform(get("/api/v1/posts/{postId}/variants/{variantId}/revisions", postId, variantId)
                .header(TENANT_HEADER, tenant));
    }

    private int revisionsInDb(String variantId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM core.post_revision WHERE post_variant_id = ?::uuid",
                Integer.class, variantId);
    }

    // ---------- когда пишется ревизия ----------

    @Test
    void createRecordsInitialRevisionForEachVariant() throws Exception {
        String post = createPost("""
                [{"platform": "TELEGRAM", "body": "tg"},
                 {"platform": "VK", "body": "vk"}]""");
        List<String> variantIds = JsonPath.read(post, "$.variants[*].id");

        for (String variantId : variantIds) {
            assertThat(revisionsInDb(variantId)).isEqualTo(1);
        }

        revisions(tenantA, id(post), variantIds.getFirst())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("tg"))
                .andExpect(jsonPath("$[0].authorType").value("USER"))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }

    @Test
    void addedVariantGetsInitialRevision() throws Exception {
        String post = createSimplePost("tg");

        String json = body(mvc.perform(post("/api/v1/posts/{postId}/variants", id(post))
                        .param("version", "0")
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"platform": "VK", "body": "Новый вариант"}"""))
                .andExpect(status().isCreated())
                .andReturn());

        List<String> vk = JsonPath.read(json, "$.variants[?(@.platform == 'VK')].id");
        revisions(tenantA, id(post), vk.getFirst())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Новый вариант"));
    }

    @Test
    void textEditsAreListedNewestFirst() throws Exception {
        String post = createSimplePost("Черновик");
        String variant = firstVariant(post);

        post = edit(post, variant, """
                {"body": "Правка 1"}""");
        edit(post, variant, """
                {"body": "Правка 2"}""");

        revisions(tenantA, id(post), variant)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].body").value("Правка 2"))
                .andExpect(jsonPath("$[1].body").value("Правка 1"))
                .andExpect(jsonPath("$[2].body").value("Черновик"));
    }

    @Test
    void hashtagOnlyOrSameTextEditDoesNotAddRevision() throws Exception {
        String post = createSimplePost("Текст");
        String variant = firstVariant(post);

        // Только хэштеги.
        post = edit(post, variant, """
                {"body": "Текст", "hashtags": ["#кофе"]}""");
        // Тот же текст ещё раз.
        edit(post, variant, """
                {"body": "Текст", "hashtags": ["#кофе"]}""");

        assertThat(revisionsInDb(variant)).isEqualTo(1);
    }

    @Test
    void rejectedEditsDoNotAddRevision() throws Exception {
        String post = createSimplePost("Исходный");
        String variant = firstVariant(post);

        // Устаревшая версия → 409.
        post = edit(post, variant, """
                {"body": "Принятая правка"}""");
        mvc.perform(put("/api/v1/posts/{postId}/variants/{variantId}", id(post), variant)
                        .param("version", "0")
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"body": "Опоздавшая правка"}"""))
                .andExpect(status().isConflict());

        // Одобренный пост → 409.
        jdbc.update("UPDATE core.post SET status = 'APPROVED' WHERE id = ?::uuid", id(post));
        mvc.perform(put("/api/v1/posts/{postId}/variants/{variantId}", id(post), variant)
                        .param("version", String.valueOf(version(post)))
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"body": "Правка после одобрения"}"""))
                .andExpect(status().isConflict());

        // Исходный + принятая правка, остальное откатилось вместе с транзакцией.
        assertThat(revisionsInDb(variant)).isEqualTo(2);
    }

    // ---------- содержимое ----------

    @Test
    void charCountCountsEmojiAsOneCharacterLikeDatabase() throws Exception {
        // ☕ в Java — 1 символ Unicode, но 1 char? Нет: U+2615 — 1 char.
        // А 🐰 (U+1F430) — 2 char (суррогатная пара), но 1 символ.
        String post = createSimplePost("🐰 кофе");
        String variant = firstVariant(post);

        String revisionsJson = body(revisions(tenantA, id(post), variant).andReturn());
        int fromRevision = JsonPath.read(revisionsJson, "$[0].charCount");

        // char_count варианта считает PostgreSQL (char_length) — должно совпасть.
        Integer fromDb = jdbc.queryForObject(
                "SELECT char_count FROM core.post_variant WHERE id = ?::uuid", Integer.class, variant);

        assertThat(fromRevision).isEqualTo(6);
        assertThat(fromRevision).isEqualTo(fromDb);
    }

    @Test
    void historyIsLimitedTo50Newest() throws Exception {
        String post = createSimplePost("r0");
        String variant = firstVariant(post);

        // 59 ревизий напрямую в БД — быстрее, чем 59 HTTP-запросов.
        for (int i = 1; i <= 59; i++) {
            jdbc.update("""
                    INSERT INTO core.post_revision (tenant_id, post_variant_id, body, author_type)
                    VALUES (?, ?::uuid, ?, 'USER')""", tenantA, variant, "r" + i);
        }

        revisions(tenantA, id(post), variant)
                .andExpect(jsonPath("$", hasSize(50)))
                // Самая новая — с наибольшим id.
                .andExpect(jsonPath("$[0].body").value("r59"))
                .andExpect(jsonPath("$[49].body").value("r10"));
    }

    // ---------- связи ----------

    @Test
    void deletingVariantDeletesItsRevisions() throws Exception {
        String post = createPost("""
                [{"platform": "TELEGRAM", "body": "tg-1"},
                 {"platform": "TELEGRAM", "body": "tg-2"}]""");
        List<String> tg2 = JsonPath.read(post, "$.variants[?(@.variantNo == 2)].id");

        mvc.perform(delete("/api/v1/posts/{postId}/variants/{variantId}", id(post), tg2.getFirst())
                        .param("version", "0")
                        .header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk());

        // FOREIGN KEY ... REFERENCES post_variant ON DELETE CASCADE (V6).
        assertThat(revisionsInDb(tg2.getFirst())).isZero();
    }

    // ---------- чужие данные ----------

    @Test
    void variantOfAnotherPostReturns404() throws Exception {
        String postA = createSimplePost("a");
        String postB = createSimplePost("b");

        revisions(tenantA, id(postA), firstVariant(postB))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherTenantGets404() throws Exception {
        String post = createSimplePost("секрет");

        revisions(tenantB, id(post), firstVariant(post))
                .andExpect(status().isNotFound());
    }
}