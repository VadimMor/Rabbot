package dev.rabbot.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Проверяет CRUD брендов внутри одной организации.
class BrandApiIT extends AbstractIntegrationTest {

    private UUID tenant;

    @BeforeEach
    void setUp() {
        tenant = createTenant("tenant-a");
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        String json = """
                {"name": "Rabbot Tea", "audience": "студенты", "tone": ["дружелюбный"]}
                """;

        MvcResult result = mvc.perform(post("/api/v1/brands")
                        .header(TENANT_HEADER, tenant)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                // Location указывает на созданный ресурс.
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/brands/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Rabbot Tea"))
                // language не передали — сервис подставил значение по умолчанию.
                .andExpect(jsonPath("$.language").value("ru"))
                // created_at и updated_at заполнила БД.
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        String id = JsonPath.read(body(result), "$.id");
        assertThat(result.getResponse().getHeader("Location")).endsWith(id);
    }

    @Test
    void keepsCyrillicArraysAndJsonb() throws Exception {
        String id = createBrand(tenant, """
                {"name": "Кофейня", "tone": ["дружелюбный", "с лёгким юмором"],
                 "forbiddenWords": ["дешёвый"], "styleRules": {"maxEmoji": 2, "capsLock": false}}
                """);

        // Проверяем прямо в базе: text[] и jsonb записались как надо.
        String tone = jdbc.queryForObject(
                "SELECT array_to_string(tone, '|') FROM core.brand WHERE id = ?::uuid", String.class, id);
        assertThat(tone).isEqualTo("дружелюбный|с лёгким юмором");

        Integer maxEmoji = jdbc.queryForObject(
                "SELECT (style_rules ->> 'maxEmoji')::int FROM core.brand WHERE id = ?::uuid", Integer.class, id);
        assertThat(maxEmoji).isEqualTo(2);

        // И через API данные возвращаются без искажений.
        MvcResult result = mvc.perform(get("/api/v1/brands/{id}", id).header(TENANT_HEADER, tenant))
                .andExpect(status().isOk())
                .andReturn();
        String json = body(result);
        List<String> toneFromApi = JsonPath.read(json, "$.tone");
        assertThat(toneFromApi).containsExactly("дружелюбный", "с лёгким юмором");
        assertThat((Boolean) JsonPath.read(json, "$.styleRules.capsLock")).isFalse();
    }

    @Test
    void listIsPagedAndNewestFirst() throws Exception {
        createBrand(tenant, """
                {"name": "A"}""");
        createBrand(tenant, """
                {"name": "B"}""");
        createBrand(tenant, """
                {"name": "C"}""");

        mvc.perform(get("/api/v1/brands").param("page", "0").param("size", "2")
                        .header(TENANT_HEADER, tenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                // Последний созданный — первым.
                .andExpect(jsonPath("$.items[0].name").value("C"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void pageSizeOver100IsRejected() throws Exception {
        mvc.perform(get("/api/v1/brands").param("size", "500").header(TENANT_HEADER, tenant))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateNameReturns409ProblemDetail() throws Exception {
        createBrand(tenant, """
                {"name": "Rabbot Tea"}""");

        mvc.perform(post("/api/v1/brands")
                        .header(TENANT_HEADER, tenant)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Rabbot Tea"}"""))
                .andExpect(status().isConflict())
                // Ошибка в формате RFC 9457.
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        // Пустое имя и язык не из двух букв.
        mvc.perform(post("/api/v1/brands")
                        .header(TENANT_HEADER, tenant)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "language": "russian"}"""))
                .andExpect(status().isBadRequest());

        // Ничего не записалось.
        Integer count = jdbc.queryForObject("SELECT count(*) FROM core.brand", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void missingOrInvalidTenantHeaderReturns400() throws Exception {
        mvc.perform(get("/api/v1/brands"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/brands").header(TENANT_HEADER, "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTenantReturns409() throws Exception {
        // Такой организации нет — сработает внешний ключ brand.tenant_id.
        mvc.perform(post("/api/v1/brands")
                        .header(TENANT_HEADER, UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Ghost"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void updateChangesFieldsAndBumpsUpdatedAt() throws Exception {
        String id = createBrand(tenant, """
                {"name": "Old", "tone": ["строгий"]}""");

        MvcResult result = mvc.perform(put("/api/v1/brands/{id}", id)
                        .header(TENANT_HEADER, tenant)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "New", "tone": ["дружелюбный"], "defaultCta": "Пишите!"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andReturn();

        String json = body(result);
        assertThat((String) JsonPath.read(json, "$.defaultCta")).isEqualTo("Пишите!");

        // Триггер set_updated_at сдвинул updated_at вперёд.
        Instant createdAt = Instant.parse(JsonPath.read(json, "$.createdAt"));
        Instant updatedAt = Instant.parse(JsonPath.read(json, "$.updatedAt"));
        assertThat(updatedAt).isAfter(createdAt);
    }

    @Test
    void renameToExistingNameReturns409() throws Exception {
        createBrand(tenant, """
                {"name": "First"}""");
        String secondId = createBrand(tenant, """
                {"name": "Second"}""");

        mvc.perform(put("/api/v1/brands/{id}", secondId)
                        .header(TENANT_HEADER, tenant)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "First"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204ThenBrandIsGone() throws Exception {
        String id = createBrand(tenant, """
                {"name": "Temp"}""");

        mvc.perform(delete("/api/v1/brands/{id}", id).header(TENANT_HEADER, tenant))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/brands/{id}", id).header(TENANT_HEADER, tenant))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}