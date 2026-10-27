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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Операции над вариантами поста: добавить, изменить, выбрать, удалить.
class PostVariantApiIT extends AbstractIntegrationTest {

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

    // Создать пост и вернуть JSON ответа (там id, version и варианты).
    private String createPost(String variantsJson) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/brands/{brandId}/posts", brandOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\": \"Тест\", \"kind\": \"TEXT\", \"variants\": " + variantsJson + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result);
    }

    // Пост с двумя вариантами Telegram (№1 выбран) и одним VK.
    private String createDefaultPost() throws Exception {
        return createPost("""
                [{"platform": "TELEGRAM", "body": "tg-1"},
                 {"platform": "TELEGRAM", "body": "tg-2"},
                 {"platform": "VK", "body": "vk-1"}]""");
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }

    private static int version(String json) {
        return JsonPath.read(json, "$.version");
    }

    // id варианта по площадке и номеру. Фильтр JsonPath возвращает список.
    private static String variantId(String json, String platform, int no) {
        List<String> ids = JsonPath.read(json,
                "$.variants[?(@.platform == '" + platform + "' && @.variantNo == " + no + ")].id");
        assertThat(ids).as("вариант %s №%d", platform, no).hasSize(1);
        return ids.getFirst();
    }

    private static boolean isSelected(String json, String platform, int no) {
        List<Boolean> flags = JsonPath.read(json,
                "$.variants[?(@.platform == '" + platform + "' && @.variantNo == " + no + ")].selected");
        return flags.getFirst();
    }

    // Сколько вариантов площадки выбрано — прямо в БД.
    private int selectedInDb(String postId, String platform) {
        return jdbc.queryForObject("""
                        SELECT count(*) FROM core.post_variant
                        WHERE post_id = ?::uuid AND platform = ? AND is_selected""",
                Integer.class, postId, platform);
    }

    private ResultActions add(String postId, int version, String json) throws Exception {
        return mvc.perform(post("/api/v1/posts/{postId}/variants", postId)
                .param("version", String.valueOf(version))
                .header(TENANT_HEADER, tenantA)
                .contentType(APPLICATION_JSON)
                .content(json));
    }

    private ResultActions edit(String postId, String variantId, int version, String json) throws Exception {
        return mvc.perform(put("/api/v1/posts/{postId}/variants/{variantId}", postId, variantId)
                .param("version", String.valueOf(version))
                .header(TENANT_HEADER, tenantA)
                .contentType(APPLICATION_JSON)
                .content(json));
    }

    private ResultActions select(String postId, String variantId, int version) throws Exception {
        return mvc.perform(post("/api/v1/posts/{postId}/variants/{variantId}/select", postId, variantId)
                .param("version", String.valueOf(version))
                .header(TENANT_HEADER, tenantA));
    }

    private ResultActions remove(String postId, String variantId, int version) throws Exception {
        return mvc.perform(delete("/api/v1/posts/{postId}/variants/{variantId}", postId, variantId)
                .param("version", String.valueOf(version))
                .header(TENANT_HEADER, tenantA));
    }

    // ---------- добавление ----------

    @Test
    void addGetsNextNumberAndIsNotSelectedWhenPlatformHasSelection() throws Exception {
        String post = createDefaultPost();

        String json = body(add(id(post), 0, """
                {"platform": "TELEGRAM", "body": "tg-3"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants", hasSize(4)))
                // Изменился вариант — выросла версия поста.
                .andExpect(jsonPath("$.version").value(1))
                .andReturn());

        assertThat(isSelected(json, "TELEGRAM", 3)).isFalse();
        assertThat(isSelected(json, "TELEGRAM", 1)).isTrue();
    }

    @Test
    void firstVariantOfNewPlatformIsSelected() throws Exception {
        String post = createDefaultPost();

        String json = body(add(id(post), 0, """
                {"platform": "INSTAGRAM", "body": "ig-1", "hashtags": ["#reels"]}""")
                .andExpect(status().isCreated())
                .andReturn());

        assertThat(isSelected(json, "INSTAGRAM", 1)).isTrue();
    }

    // ---------- изменение текста ----------

    @Test
    void editUpdatesTextHashtagsAndCharCount() throws Exception {
        String post = createDefaultPost();
        String tg2 = variantId(post, "TELEGRAM", 2);

        String json = body(edit(id(post), tg2, 0, """
                {"body": "Новый текст", "hashtags": ["#кофе", "#осень"]}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn());

        List<String> bodies = JsonPath.read(json, "$.variants[?(@.id == '" + tg2 + "')].body");
        List<Integer> counts = JsonPath.read(json, "$.variants[?(@.id == '" + tg2 + "')].charCount");
        List<List<String>> tags = JsonPath.read(json, "$.variants[?(@.id == '" + tg2 + "')].hashtags");
        assertThat(bodies).containsExactly("Новый текст");
        // char_count пересчитала БД (GENERATED-колонка) после UPDATE.
        assertThat(counts).containsExactly(11);
        assertThat(tags.getFirst()).containsExactly("#кофе", "#осень");

        // Остальные варианты не тронуты.
        List<String> vk = JsonPath.read(json, "$.variants[?(@.platform == 'VK')].body");
        assertThat(vk).containsExactly("vk-1");
    }

    // ---------- выбор ----------

    @Test
    void selectSwitchesSelectionWithinPlatform() throws Exception {
        String post = createDefaultPost();
        String tg2 = variantId(post, "TELEGRAM", 2);

        String json = body(select(id(post), tg2, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn());

        assertThat(isSelected(json, "TELEGRAM", 1)).isFalse();
        assertThat(isSelected(json, "TELEGRAM", 2)).isTrue();
        // В БД ровно один выбранный — схема "снять, flush, выбрать" сработала.
        assertThat(selectedInDb(id(post), "TELEGRAM")).isEqualTo(1);
        // Выбор у VK не изменился.
        assertThat(isSelected(json, "VK", 1)).isTrue();
    }

    @Test
    void selectingAlreadySelectedVariantKeepsIt() throws Exception {
        String post = createDefaultPost();
        String tg1 = variantId(post, "TELEGRAM", 1);

        String json = body(select(id(post), tg1, 0).andExpect(status().isOk()).andReturn());

        assertThat(isSelected(json, "TELEGRAM", 1)).isTrue();
        assertThat(selectedInDb(id(post), "TELEGRAM")).isEqualTo(1);
    }

    // ---------- удаление ----------

    @Test
    void deletingSelectedVariantSelectsLowestRemaining() throws Exception {
        String post = createPost("""
                [{"platform": "TELEGRAM", "body": "tg-1"},
                 {"platform": "TELEGRAM", "body": "tg-2"},
                 {"platform": "TELEGRAM", "body": "tg-3"}]""");
        String tg3 = variantId(post, "TELEGRAM", 3);

        // Выбираем №3 (version 0 → 1), потом удаляем его (1 → 2).
        select(id(post), tg3, 0).andExpect(status().isOk());

        String json = body(remove(id(post), tg3, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andReturn());

        // Из оставшихся №1 и №2 выбран вариант с наименьшим номером.
        assertThat(isSelected(json, "TELEGRAM", 1)).isTrue();
        assertThat(isSelected(json, "TELEGRAM", 2)).isFalse();
        assertThat(selectedInDb(id(post), "TELEGRAM")).isEqualTo(1);
    }

    @Test
    void deletingNotSelectedVariantKeepsSelection() throws Exception {
        String post = createDefaultPost();
        String tg2 = variantId(post, "TELEGRAM", 2);

        String json = body(remove(id(post), tg2, 0).andExpect(status().isOk()).andReturn());

        assertThat(isSelected(json, "TELEGRAM", 1)).isTrue();
        Integer rows = jdbc.queryForObject(
                "SELECT count(*) FROM core.post_variant WHERE id = ?::uuid", Integer.class, tg2);
        assertThat(rows).isZero();
    }

    @Test
    void deletingLastVariantOfPlatformLeavesNoneForIt() throws Exception {
        String post = createDefaultPost();
        String vk1 = variantId(post, "VK", 1);

        remove(id(post), vk1, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[?(@.platform == 'VK')]", hasSize(0)))
                .andExpect(jsonPath("$.variants", hasSize(2)));
    }

    // ---------- версия ----------

    @Test
    void everyOperationRequiresCurrentVersion() throws Exception {
        String post = createDefaultPost();
        String postId = id(post);
        String tg2 = variantId(post, "TELEGRAM", 2);

        // Кто-то другой уже изменил пост: version 0 → 1.
        edit(postId, tg2, 0, """
                {"body": "Правка Анны"}""").andExpect(status().isOk());

        // Все операции со старой версией 0 → 409.
        add(postId, 0, """
                {"platform": "VK", "body": "x"}""").andExpect(status().isConflict());
        edit(postId, tg2, 0, """
                {"body": "Правка Ильи"}""").andExpect(status().isConflict());
        select(postId, tg2, 0).andExpect(status().isConflict());
        remove(postId, tg2, 0).andExpect(status().isConflict());

        // Ничего из этого не применилось.
        String current = body(mvc.perform(get("/api/v1/posts/{id}", postId).header(TENANT_HEADER, tenantA))
                .andReturn());
        assertThat(version(current)).isEqualTo(1);
        assertThat((List<String>) JsonPath.read(current, "$.variants[?(@.id == '" + tg2 + "')].body"))
                .containsExactly("Правка Анны");
        assertThat(isSelected(current, "TELEGRAM", 1)).isTrue();
    }

    @Test
    void missingVersionReturns400() throws Exception {
        String post = createDefaultPost();

        mvc.perform(post("/api/v1/posts/{postId}/variants", id(post))
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"platform": "VK", "body": "x"}"""))
                .andExpect(status().isBadRequest());
    }

    // ---------- статус поста ----------

    @Test
    void approvedPostVariantsCannotBeChanged() throws Exception {
        String post = createDefaultPost();
        String postId = id(post);
        String tg2 = variantId(post, "TELEGRAM", 2);
        jdbc.update("UPDATE core.post SET status = 'APPROVED' WHERE id = ?::uuid", postId);

        add(postId, 0, """
                {"platform": "VK", "body": "x"}""").andExpect(status().isConflict());
        edit(postId, tg2, 0, """
                {"body": "Поздно"}""").andExpect(status().isConflict());
        select(postId, tg2, 0).andExpect(status().isConflict());
        remove(postId, tg2, 0).andExpect(status().isConflict());
    }

    // ---------- входные данные ----------

    @Test
    void invalidBodyReturns400() throws Exception {
        String post = createDefaultPost();
        String tg2 = variantId(post, "TELEGRAM", 2);

        // Хэштег без решётки.
        edit(id(post), tg2, 0, """
                {"body": "x", "hashtags": ["кофе"]}""").andExpect(status().isBadRequest());
        // Нет текста.
        edit(id(post), tg2, 0, """
                {"hashtags": []}""").andExpect(status().isBadRequest());
        // Нет площадки при добавлении.
        add(id(post), 0, """
                {"body": "x"}""").andExpect(status().isBadRequest());
    }

    // ---------- чужие данные ----------

    @Test
    void variantOfAnotherPostReturns404() throws Exception {
        String postA = createDefaultPost();
        String postB = createDefaultPost();
        String variantOfB = variantId(postB, "TELEGRAM", 2);

        // Вариант существует, но принадлежит другому посту.
        edit(id(postA), variantOfB, 0, """
                {"body": "Подмена"}""").andExpect(status().isNotFound());
        remove(id(postA), variantOfB, 0).andExpect(status().isNotFound());
    }

    @Test
    void otherTenantGets404() throws Exception {
        String post = createDefaultPost();
        String tg2 = variantId(post, "TELEGRAM", 2);

        mvc.perform(put("/api/v1/posts/{postId}/variants/{variantId}", id(post), tg2)
                        .param("version", "0")
                        .header(TENANT_HEADER, tenantB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"body": "Взлом"}"""))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/posts/{postId}/variants/{variantId}", id(post), tg2)
                        .param("version", "0")
                        .header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        Integer variants = jdbc.queryForObject(
                "SELECT count(*) FROM core.post_variant WHERE post_id = ?::uuid", Integer.class, id(post));
        assertThat(variants).isEqualTo(3);
    }
}