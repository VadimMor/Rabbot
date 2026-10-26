package dev.rabbot.api.dto.response;

import dev.rabbot.api.enums.SocialPlatform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostVariantResponse(
        UUID id,
        SocialPlatform platform,
        int variantNo,
        String body,
        List<String> hashtags,
        Integer charCount,
        boolean selected,
        String draftModel,
        String finalModel,
        Instant updatedAt
) {
}