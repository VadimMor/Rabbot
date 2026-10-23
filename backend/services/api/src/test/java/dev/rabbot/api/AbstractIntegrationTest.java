package dev.rabbot.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Общая база для всех интеграционных тестов.
// Все наследники используют один и тот же Spring-контекст (Spring кэширует его),
// поэтому контейнер PostgreSQL поднимается один раз на весь прогон.
@SpringBootTest                 // поднять приложение целиком
@AutoConfigureMockMvc           // дать MockMvc для HTTP-запросов
@ActiveProfiles("test")         // подключить application-test.yml
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    protected MockMvc mvc;

    // Прямой доступ к БД: подготовить данные и проверить результат в обход API.
    @Autowired
    protected JdbcTemplate jdbc;

    // Перед каждым тестом — пустая база. CASCADE очищает и все таблицы,
    // ссылающиеся на tenant (brand, channel, post...). Тесты не влияют друг на друга.
    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE core.tenant CASCADE");
    }

    // Создать организацию напрямую в БД (API для организаций пока нет).
    protected UUID createTenant(String slug) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO core.tenant (id, slug, name) VALUES (?, ?, ?)",
                id, slug, "Tenant " + slug);
        return id;
    }

    // Создать бренд через API, проверить 201 и вернуть его id.
    protected String createBrand(UUID tenantId, String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/brands")
                        .header(TENANT_HEADER, tenantId)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(body(result), "$.id");
    }

    // Тело ответа строкой в UTF-8 (по умолчанию MockMvc читает в ISO-8859-1,
    // и кириллица превратилась бы в мусор).
    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}