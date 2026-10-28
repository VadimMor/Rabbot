package dev.rabbot.api.config;

import dev.rabbot.api.utils.CurrentTenant;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Описание API для springdoc: заголовок, лицензия, X-Tenant-Id, формат ошибок.
@Configuration
public class OpenApiConfig {

    public static final String TENANT_SCHEME = "tenant";

    static {
        // Параметр "@CurrentTenant UUID tenantId" заполняет наш резолвер из заголовка.
        // springdoc об этом не знает и показал бы его как query-параметр ?tenantId=.
        // Скрываем: заголовок описан ниже как схема авторизации.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentTenant.class);
    }

    @Bean
    OpenAPI rabbotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rabbot API")
                        .version("v1")
                        .description("""
                                Бренды, каналы соцсетей, посты и их варианты.
                                Все запросы выполняются от имени организации: нажмите **Authorize** \
                                и введите её UUID (для демо-данных — `00000000-0000-0000-0000-000000000001`).""")
                        .license(new License()
                                .name("Apache-2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        // Заголовок X-Tenant-Id как "API-ключ в заголовке" —
                        // так Swagger UI покажет кнопку Authorize.
                        // После Keycloak здесь будет OAuth2 / OIDC.
                        .addSecuritySchemes(TENANT_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Tenant-Id")
                                .description("UUID организации. Временно, до подключения Keycloak.")))
                // Применить схему ко всем операциям.
                .addSecurityItem(new SecurityRequirement().addList(TENANT_SCHEME));
    }

    // Добавить во все операции типовые ошибки в формате ProblemDetail (RFC 9457).
    // Их возвращает GlobalExceptionHandler, но springdoc сам о них не узнает.
    @Bean
    OpenApiCustomizer problemDetailResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas("ProblemDetail", new ObjectSchema()
                    .description("Ошибка в формате RFC 9457")
                    .addProperty("type", new StringSchema().example("about:blank"))
                    .addProperty("title", new StringSchema().example("Conflict"))
                    .addProperty("status", new IntegerSchema().example(409))
                    .addProperty("detail", new StringSchema().example("Пост уже изменён (версия 3). Обновите страницу и повторите."))
                    .addProperty("instance", new StringSchema().example("/api/v1/posts/…")));

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                // putIfAbsent — не перетираем то, что описано в @ApiResponse у метода.
                responses.putIfAbsent("400", problem("Неверные данные или заголовок X-Tenant-Id"));
                responses.putIfAbsent("404", problem("Не найдено (или принадлежит другой организации)"));
                responses.putIfAbsent("409", problem("Конфликт: дубликат, устаревшая версия или недопустимый статус"));
            }));
        };
    }

    private static ApiResponse problem(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }
}