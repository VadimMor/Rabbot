package dev.rabbot.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// То, что сервис отдаёт наружу. tenantId не отдаём: клиент и так его знает.
public record BrandResponse(
        UUID id,
        String name,
        String audience,
        String language,
        List<String> tone,
        List<String> forbiddenWords,
        String defaultCta,
        Map<String, Object> styleRules,
        Instant createdAt,
        Instant updatedAt
) {
}