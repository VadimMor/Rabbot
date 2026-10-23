package dev.rabbot.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Главная проверка мультитенантности: организация B никак не может
// увидеть или изменить бренды организации A, даже зная их id.
class TenantIsolationIT extends AbstractIntegrationTest {

    private UUID tenantA;
    private UUID tenantB;
    private String brandOfA;

    @BeforeEach
    void setUp() throws Exception {
        tenantA = createTenant("tenant-a");
        tenantB = createTenant("tenant-b");
        brandOfA = createBrand(tenantA, """
                {"name": "Secret Brand"}""");
    }

    @Test
    void otherTenantDoesNotSeeBrandInList() throws Exception {
        mvc.perform(get("/api/v1/brands").header(TENANT_HEADER, tenantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void otherTenantGets404ById() throws Exception {
        // 404, а не 403: не раскрываем даже сам факт существования бренда.
        mvc.perform(get("/api/v1/brands/{id}", brandOfA).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherTenantCannotUpdate() throws Exception {
        mvc.perform(put("/api/v1/brands/{id}", brandOfA)
                        .header(TENANT_HEADER, tenantB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Hacked"}"""))
                .andExpect(status().isNotFound());

        String name = jdbc.queryForObject(
                "SELECT name FROM core.brand WHERE id = ?::uuid", String.class, brandOfA);
        assertThat(name).isEqualTo("Secret Brand");
    }

    @Test
    void otherTenantCannotDelete() throws Exception {
        mvc.perform(delete("/api/v1/brands/{id}", brandOfA).header(TENANT_HEADER, tenantB))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/brands/{id}", brandOfA).header(TENANT_HEADER, tenantA))
                .andExpect(status().isOk());
    }

    @Test
    void sameNameIsAllowedInDifferentTenants() throws Exception {
        // UNIQUE (tenant_id, name): имя уникально только внутри организации.
        createBrand(tenantB, """
                {"name": "Secret Brand"}""");

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM core.brand WHERE name = 'Secret Brand'", Integer.class);
        assertThat(count).isEqualTo(2);
    }
}