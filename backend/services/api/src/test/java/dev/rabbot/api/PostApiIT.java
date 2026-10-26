package dev.rabbot.api;

import com.jayway.jsonpath.JsonPath;
import dev.rabbot.api.model.Post;
import dev.rabbot.api.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Посты и варианты: создание, чтение, фильтры, переименование с версией,
// статусы, изоляция организаций, каскадное удаление.
class PostApiIT extends AbstractIntegrationTest {

    // Пост с тремя вариантами: два под Telegram, один под VK.
    private static final String POST_WITH_VARIANTS = """
            {
              "title": "Осеннее меню",
              "kind": "TEXT",
              "variants": [
                {"platform": "TELEGRAM", "body": "Тыквенный латте вернулся!", "hashtags": ["#осень", "#кофе"]},
                {"platform": "TELEGRAM", "body": "Второй вариант"},
                {"platform": "VK", "body": "Привет"}
              ]
            }
            """;

    @Autowired
    private PostRepository postRepository;

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

    // Создать пост через API и вернуть его id.
    private String createPost(String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(body(result), "$.id");
    }

    private String simplePost(String title) throws Exception {
        return createPost("{\"title\": \"" + title + "\", \"kind\": \"TEXT\"}");
    }

    // ---------- создание ----------

    @Test
    void createNumbersVariantsAndSelectsFirstPerPlatform() throws Exception {
        mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content(POST_WITH_VARIANTS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.brandId").value(brandOfA))
                .andExpect(jsonPath("$.variants", hasSize(3)))
                // Telegram: №1 выбран, №2 — нет.
                .andExpect(jsonPath("$.variants[0].platform").value("TELEGRAM"))
                .andExpect(jsonPath("$.variants[0].variantNo").value(1))
                .andExpect(jsonPath("$.variants[0].selected").value(true))
                .andExpect(jsonPath("$.variants[1].variantNo").value(2))
                .andExpect(jsonPath("$.variants[1].selected").value(false))
                // VK: единственный вариант, номер 1, выбран.
                .andExpect(jsonPath("$.variants[2].platform").value("VK"))
                .andExpect(jsonPath("$.variants[2].variantNo").value(1))
                .andExpect(jsonPath("$.variants[2].selected").value(true));
    }

    @Test
    void charCountIsCalculatedByDatabase() throws Exception {
        String id = createPost(POST_WITH_VARIANTS);

        // char_count — GENERATED-колонка: "Привет" = 6 символов (не байтов).
        Integer count = jdbc.queryForObject(
                "SELECT char_count FROM core.post_variant WHERE post_id = ?::uuid AND platform = 'VK'",
                Integer.class, id);
        assertThat(count).isEqualTo(6);

        mvc.perform(get("/api/v1/posts/{id}", id).header(TENANT_HEADER, tenantA))
                .andExpect(jsonPath("$.variants[?(@.platform == 'VK')].charCount").value(6));
    }

    @Test
    void createWithoutVariantsIsAllowed() throws Exception {
        String id = simplePost("Идея на потом");

        mvc.perform(get("/api/v1/posts/{id}", id).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants", hasSize(0)));
    }

    @Test
    void createRejectsInvalidBody() throws Exception {
        String[] invalid = {
                // пустой заголовок
                """
                {"title": " ", "kind": "TEXT"}""",
                // нет kind
                """
                {"title": "Пост"}""",
                // неизвестная площадка
                """
                {"title": "Пост", "kind": "TEXT", "variants": [{"platform": "MYSPACE", "body": ""}]}""",
                // хэштег без решётки
                """
                {"title": "Пост", "kind": "TEXT", "variants": [{"platform": "VK", "body": "", "hashtags": ["кофе"]}]}"""
        };

        for (String json : invalid) {
            mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                            .header(TENANT_HEADER, tenantA)
                            .contentType(APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        Integer posts = jdbc.queryForObject("SELECT count(*) FROM core.post", Integer.class);
        assertThat(posts).isZero();
    }

    @Test
    void createWithUnknownTrendReturns409() throws Exception {
        // trend_id ссылается на trend(id) — внешний ключ не пропустит выдуманный id.
        mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\": \"Пост\", \"kind\": \"TEXT\", \"trendId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- чтение ----------

    @Test
    void getReturnsVariantsOrderedByPlatformAndNumber() throws Exception {
        // Создаём в "неправильном" порядке: VK раньше Telegram.
        String id = createPost("""
                {"title": "Порядок", "kind": "TEXT", "variants": [
                  {"platform": "VK", "body": "vk"},
                  {"platform": "TELEGRAM", "body": "tg-1"},
                  {"platform": "TELEGRAM", "body": "tg-2"}
                ]}""");

        // @OrderBy("platform ASC, variantNo ASC") при загрузке из БД.
        mvc.perform(get("/api/v1/posts/{id}", id).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].body").value("tg-1"))
                .andExpect(jsonPath("$.variants[1].body").value("tg-2"))
                .andExpect(jsonPath("$.variants[2].body").value("vk"));
    }

    @Test
    void listFiltersByStatusAndPages() throws Exception {
        simplePost("Первый");
        String second = simplePost("Второй");
        simplePost("Третий");
        // Статусы через API пока не меняются — двигаем пост в ревью напрямую в БД.
        jdbc.update("UPDATE core.post SET status = 'IN_REVIEW' WHERE id = ?::uuid", second);

        mvc.perform(get("/api/v1/brands/{brandId}/posts", brandOfA)
                        .param("status", "IN_REVIEW")
                        .header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Второй"))
                // В списке вариантов нет — только краткая карточка.
                .andExpect(jsonPath("$.items[0].variants").doesNotExist());

        mvc.perform(get("/api/v1/brands/{brandId}/posts", brandOfA)
                        .param("size", "2")
                        .header(TENANT_HEADER, tenantA))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listWithUnknownStatusReturns400() throws Exception {
        mvc.perform(get("/api/v1/brands/{brandId}/posts", brandOfA)
                        .param("status", "WRONG")
                        .header(TENANT_HEADER, tenantA))
                .andExpect(status().isBadRequest());
    }

    // ---------- переименование и версия ----------

    @Test
    void renameWithCurrentVersionIncrementsVersion() throws Exception {
        String id = simplePost("Старое имя");

        String json = body(mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Новое имя", "version": 0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое имя"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn());

        Instant createdAt = Instant.parse(JsonPath.read(json, "$.createdAt"));
        Instant updatedAt = Instant.parse(JsonPath.read(json, "$.updatedAt"));
        assertThat(updatedAt).isAfter(createdAt);
    }

    @Test
    void renameWithStaleVersionReturns409() throws Exception {
        String id = simplePost("Исходное");

        // Первый редактор сохранил — версия стала 1.
        mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Правка Анны", "version": 0}"""))
                .andExpect(status().isOk());

        // Второй всё ещё видит версию 0 → конфликт, правка Анны не затёрта.
        mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Правка Ильи", "version": 0}"""))
                .andExpect(status().isConflict());

        String title = jdbc.queryForObject("SELECT title FROM core.post WHERE id = ?::uuid", String.class, id);
        assertThat(title).isEqualTo("Правка Анны");
    }

    @Test
    void hibernateVersionBlocksLostUpdate() throws Exception {
        String id = simplePost("Гонка");

        // Копия поста, прочитанная "до" чужой правки (version = 0).
        // Вне транзакции сущность отсоединена от Hibernate (detached).
        Post stale = postRepository.findById(UUID.fromString(id)).orElseThrow();

        // Кто-то другой успел сохранить → в БД version = 1.
        mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Свежая правка", "version": 0}"""))
                .andExpect(status().isOk());

        // Сохраняем устаревшую копию в обход проверки в сервисе.
        // Hibernate сравнит version 0 с version 1 в БД и откажет.
        stale.rename("Устаревшая правка");
        assertThatThrownBy(() -> postRepository.save(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        String title = jdbc.queryForObject("SELECT title FROM core.post WHERE id = ?::uuid", String.class, id);
        assertThat(title).isEqualTo("Свежая правка");
    }

    @Test
    void scheduledPostCannotBeRenamed() throws Exception {
        String id = simplePost("Уже в расписании");
        jdbc.update("UPDATE core.post SET status = 'SCHEDULED' WHERE id = ?::uuid", id);

        mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Поздно", "version": 0}"""))
                .andExpect(status().isConflict());
    }

    // ---------- изоляция организаций ----------

    @Test
    void otherTenantCannotReadCreateOrRename() throws Exception {
        String id = createPost(POST_WITH_VARIANTS);

        mvc.perform(get("/api/v1/posts/{id}", id).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/brands/{brandId}/posts", brandOfA).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        // Создать пост в чужом бренде нельзя.
        mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Подброшенный", "kind": "TEXT"}"""))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/posts/{id}", id)
                        .header(TENANT_HEADER, tenantB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title": "Взлом", "version": 0}"""))
                .andExpect(status().isNotFound());

        Integer posts = jdbc.queryForObject("SELECT count(*) FROM core.post", Integer.class);
        assertThat(posts).isEqualTo(1);
    }

    // ---------- связи ----------

    @Test
    void deletingBrandDeletesPostsAndVariants() throws Exception {
        createPost(POST_WITH_VARIANTS);

        mvc.perform(delete("/api/v1/brands/{id}", brandOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isNoContent());

        // brand → post → post_variant: каскад ON DELETE CASCADE на двух уровнях.
        Integer posts = jdbc.queryForObject("SELECT count(*) FROM core.post", Integer.class);
        Integer variants = jdbc.queryForObject("SELECT count(*) FROM core.post_variant", Integer.class);
        assertThat(posts).isZero();
        assertThat(variants).isZero();
    }

    @Test
    void onlyOneSelectedVariantPerPlatformInDatabase() throws Exception {
        String id = createPost(POST_WITH_VARIANTS);

        // Частичный уникальный индекс post_variant_one_selected_idx:
        // второй выбранный вариант для той же площадки БД не пропустит.
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE core.post_variant SET is_selected = true WHERE post_id = ?::uuid AND platform = 'TELEGRAM'", id))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }
}