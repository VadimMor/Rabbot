package dev.rabbot.api.dto.response;

import dev.rabbot.api.enums.PostKind;
import dev.rabbot.api.enums.PostStatus;

import java.time.Instant;
import java.util.UUID;

// Пост в списке — без вариантов. Так список строится одним SQL-запросом,
// без подгрузки вариантов для каждого поста (проблема N+1).
public record PostSummaryResponse(
        UUID id,
        UUID brandId,
        UUID trendId,
        PostKind kind,
        String title,
        PostStatus status,
        Integer brandScore,
        Instant createdAt,
        Instant updatedAt
) {
}