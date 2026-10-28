package dev.rabbot.api.dto.response;

import dev.rabbot.api.enums.RevisionAuthor;

import java.time.Instant;
import java.util.UUID;

public record RevisionResponse(
        Long id,
        String body,
        int charCount,
        RevisionAuthor authorType,
        UUID authorId,
        String model,
        Instant createdAt
) {
}