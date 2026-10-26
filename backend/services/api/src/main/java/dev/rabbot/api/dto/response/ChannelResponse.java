package dev.rabbot.api.dto.response;

import dev.rabbot.api.enums.ChannelStatus;
import dev.rabbot.api.enums.SocialPlatform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Канал для API. secretRef специально не отдаём: это путь к секрету в Vault.
public record ChannelResponse(
        UUID id,
        UUID brandId,
        SocialPlatform platform,
        String externalAccountId,
        String handle,
        String displayName,
        ChannelStatus status,
        boolean autoPublish,
        int dailyPostLimit,
        String timezone,
        Instant tokenExpiresAt,
        List<String> scopes,
        Integer audienceSize,
        Instant connectedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}