package dev.rabbot.api.dto.response;

import dev.rabbot.api.enums.PostKind;
import dev.rabbot.api.enums.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Пост целиком — с вариантами. version нужен клиенту для оптимистичной
// блокировки: при сохранении он вернёт ту версию, которую видел.
public record PostResponse(
        UUID id,
        UUID brandId,
        UUID trendId,
        PostKind kind,
        String title,
        PostStatus status,
        Integer brandScore,
        UUID createdBy,
        UUID approvedBy,
        Instant approvedAt,
        int version,
        Instant createdAt,
        Instant updatedAt,
        List<PostVariantResponse> variants
) {
}