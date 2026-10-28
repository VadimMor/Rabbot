package dev.rabbot.api;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Документация API собирается и описывает то, что нужно.
class OpenApiIT extends AbstractIntegrationTest {

    @Test
    void apiDocsDescribeAllControllers() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                // Документация доступна без X-Tenant-Id.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Rabbot API"))
                .andExpect(jsonPath("$.paths['/api/v1/brands']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/brands/{brandId}/channels']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/variants/{variantId}/revisions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}/approve']").exists());
    }

    @Test
    void tenantIsHeaderSchemeNotQueryParameter() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.tenant.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.tenant.in").value("header"))
                .andExpect(jsonPath("$.components.securitySchemes.tenant.name").value("X-Tenant-Id"))
                .andExpect(jsonPath("$.paths['/api/v1/brands'].get.parameters[?(@.name == 'tenantId')]", hasSize(0)));
    }

    @Test
    void errorsAreDescribedAsProblemDetail() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{id}'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/brands/{id}'].get.responses['404']").exists());
    }
}