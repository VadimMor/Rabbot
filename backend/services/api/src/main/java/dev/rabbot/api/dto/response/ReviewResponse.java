package dev.rabbot.api.dto.response;

import dev.rabbot.api.enums.ReviewDecision;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        ReviewDecision decision,
        String comment,
        UUID reviewerId,
        Instant createdAt
) {
}