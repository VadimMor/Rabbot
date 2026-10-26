package dev.rabbot.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Каналы: чтение, настройки, отключение, изоляция организаций и каскадное удаление.
class ChannelApiIT extends AbstractIntegrationTest {

    private UUID tenantA;
    private UUID tenantB;
    private String brandOfA;
    private UUID telegramOfA;

    @BeforeEach
    void setUp() throws Exception {
        tenantA = createTenant("tenant-a");
        tenantB = createTenant("tenant-b");
        brandOfA = createBrand(tenantA, """
                {"name": "Rabbot Coffee"}""");
        telegramOfA = insertChannel(tenantA, brandOfA, "TELEGRAM", "-100111");
    }

    // Канал создаётся OAuth-подключением, которого ещё нет,
    // поэтому в тестах вставляем его прямо в БД.
    private UUID insertChannel(UUID tenantId, String brandId, String platform, String externalId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO core.channel
                            (id, tenant_id, brand_id, platform, external_account_id, display_name, secret_ref, scopes)
                        VALUES (?, ?, ?::uuid, ?, ?, ?, ?, ARRAY['post'])
                        """,
                id, tenantId, brandId, platform, externalId, "Test " + platform, "secret/test/" + externalId);
        return id;
    }

    // ---------- чтение ----------

    @Test
    void listReturnsChannelsOfBrandInConnectionOrder() throws Exception {
        insertChannel(tenantA, brandOfA, "VK", "-200222");

        mvc.perform(get("/api/v1/brands/{brandId}/channels", brandOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Сначала подключённый раньше.
                .andExpect(jsonPath("$[0].platform").value("TELEGRAM"))
                .andExpect(jsonPath("$[1].platform").value("VK"))
                .andExpect(jsonPath("$[0].brandId").value(brandOfA))
                // Значения по умолчанию из БД.
                .andExpect(jsonPath("$[0].status").value("CONNECTED"))
                .andExpect(jsonPath("$[0].dailyPostLimit").value(10))
                // Путь к секрету в Vault наружу не отдаётся.
                .andExpect(jsonPath("$[0].secretRef").doesNotExist());
    }

    @Test
    void listForUnknownBrandReturns404() throws Exception {
        mvc.perform(get("/api/v1/brands/{brandId}/channels", UUID.randomUUID()).header(TENANT_HEADER, tenantA))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReturnsChannel() throws Exception {
        mvc.perform(get("/api/v1/channels/{id}", telegramOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(telegramOfA.toString()))
                .andExpect(jsonPath("$.externalAccountId").value("-100111"))
                .andExpect(jsonPath("$.scopes[0]").value("post"));
    }

    // ---------- настройки ----------

    @Test
    void patchChangesOnlyGivenFields() throws Exception {
        String json = body(mvc.perform(patch("/api/v1/channels/{id}", telegramOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"dailyPostLimit": 3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyPostLimit").value(3))
                // Не передавали — не изменилось.
                .andExpect(jsonPath("$.timezone").value("Europe/Moscow"))
                .andExpect(jsonPath("$.autoPublish").value(true))
                .andReturn());

        Instant createdAt = Instant.parse(com.jayway.jsonpath.JsonPath.read(json, "$.createdAt"));
        Instant updatedAt = Instant.parse(com.jayway.jsonpath.JsonPath.read(json, "$.updatedAt"));
        assertThat(updatedAt).isAfter(createdAt);
    }

    @Test
    void patchSetsTimezoneAndAutoPublish() throws Exception {
        mvc.perform(patch("/api/v1/channels/{id}", telegramOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"timezone": "Asia/Yekaterinburg", "autoPublish": false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Yekaterinburg"))
                .andExpect(jsonPath("$.autoPublish").value(false));
    }

    @Test
    void patchRejectsInvalidValues() throws Exception {
        // Несуществующий часовой пояс.
        mvc.perform(patch("/api/v1/channels/{id}", telegramOfA)
                        .header(TENANT_HEADER, tenantA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"timezone": "Mars/Olympus"}"""))
                .andExpect(status().isBadRequest());

        // Лимит вне 1..100.
        for (String limit : new String[]{"0", "101"}) {
            mvc.perform(patch("/api/v1/channels/{id}", telegramOfA)
                            .header(TENANT_HEADER, tenantA)
                            .contentType(APPLICATION_JSON)
                            .content("{\"dailyPostLimit\": " + limit + "}"))
                    .andExpect(status().isBadRequest());
        }

        // В БД ничего не поменялось.
        Integer dbLimit = jdbc.queryForObject(
                "SELECT daily_post_limit FROM core.channel WHERE id = ?", Integer.class, telegramOfA);
        assertThat(dbLimit).isEqualTo(10);
    }

    // ---------- отключение ----------

    @Test
    void disconnectSetsStatusAndStopsAutoPublish() throws Exception {
        mvc.perform(post("/api/v1/channels/{id}/disconnect", telegramOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONNECTED"))
                .andExpect(jsonPath("$.autoPublish").value(false));

        String dbStatus = jdbc.queryForObject(
                "SELECT status FROM core.channel WHERE id = ?", String.class, telegramOfA);
        assertThat(dbStatus).isEqualTo("DISCONNECTED");
    }

    // ---------- изоляция организаций ----------

    @Test
    void otherTenantCannotSeeOrChangeChannel() throws Exception {
        mvc.perform(get("/api/v1/brands/{brandId}/channels", brandOfA).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/channels/{id}", telegramOfA).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/channels/{id}", telegramOfA)
                        .header(TENANT_HEADER, tenantB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"autoPublish": false}"""))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/channels/{id}/disconnect", telegramOfA).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        // Канал организации A не тронут.
        String dbStatus = jdbc.queryForObject(
                "SELECT status FROM core.channel WHERE id = ?", String.class, telegramOfA);
        assertThat(dbStatus).isEqualTo("CONNECTED");
    }

    // ---------- связь с брендом ----------

    @Test
    void deletingBrandDeletesItsChannels() throws Exception {
        // FOREIGN KEY (tenant_id, brand_id) ... ON DELETE CASCADE в V4.
        mvc.perform(delete("/api/v1/brands/{id}", brandOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isNoContent());

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM core.channel WHERE brand_id = ?::uuid", Integer.class, brandOfA);
        assertThat(count).isZero();
    }

    @Test
    void sameExternalAccountCannotBeConnectedTwice() {
        // UNIQUE (tenant_id, platform, external_account_id): один аккаунт — один канал.
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DuplicateKeyException.class,
                () -> insertChannel(tenantA, brandOfA, "TELEGRAM", "-100111"));
    }
}