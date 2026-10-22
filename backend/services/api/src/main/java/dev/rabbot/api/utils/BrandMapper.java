package dev.rabbot.api.utils;

import dev.rabbot.api.dto.request.BrandRequest;
import dev.rabbot.api.dto.response.BrandResponse;
import dev.rabbot.api.model.Brand;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Переводит DTO ↔ сущность. Отдельный класс, чтобы сервис не был забит
// копированием полей. @Component, потому что нужен JsonMapper из Spring.
@Component
public class BrandMapper {

    // Тип для чтения JSON в Map<String, Object>.
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    // Spring Boot 4 использует Jackson 3 (пакет tools.jackson).
    // Его исключения непроверяемые, поэтому try/catch не нужен.
    private final JsonMapper jsonMapper;

    public BrandMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // Новый бренд из запроса.
    public Brand toNewEntity(UUID tenantId, BrandRequest request) {
        Brand brand = new Brand(tenantId);
        apply(brand, request);
        return brand;
    }

    // Копирует поля запроса в сущность (и при создании, и при PUT).
    public void apply(Brand brand, BrandRequest request) {
        brand.setName(request.name().strip());
        brand.setAudience(request.audience());
        brand.setLanguage(request.language() == null ? "ru" : request.language());
        brand.setTone(copyOrEmpty(request.tone()));
        brand.setForbiddenWords(copyOrEmpty(request.forbiddenWords()));
        brand.setDefaultCta(request.defaultCta());

        Map<String, Object> rules = request.styleRules() == null ? Map.of() : request.styleRules();
        brand.setStyleRules(jsonMapper.writeValueAsString(rules));
    }

    // Сущность → ответ API.
    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getAudience(),
                brand.getLanguage(),
                List.copyOf(brand.getTone()),
                List.copyOf(brand.getForbiddenWords()),
                brand.getDefaultCta(),
                jsonMapper.readValue(brand.getStyleRules(), MAP_TYPE),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }

    // null → пустой список. Иначе изменяемая копия: Hibernate не любит
    // неизменяемые списки вроде List.of().
    private static List<String> copyOrEmpty(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}