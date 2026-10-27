package dev.rabbot.api.dto.request;

import jakarta.validation.constraints.Size;

// Необязательный комментарий при одобрении.
public record ReviewRequest(
        @Size(max = 2000)
        String comment
) {
}