package dev.rabbot.api.services;

import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.ReviewResponse;

import java.util.List;
import java.util.UUID;

// Переходы поста по жизненному циклу и история ревью.
public interface PostWorkflowService {

    PostResponse submit(UUID tenantId, UUID postId, int version);

    PostResponse approve(UUID tenantId, UUID postId, int version, String comment);

    PostResponse requestChanges(UUID tenantId, UUID postId, int version, String comment);

    PostResponse archive(UUID tenantId, UUID postId, int version);

    List<ReviewResponse> reviews(UUID tenantId, UUID postId);
}