package dev.rabbot.api.repository;

import dev.rabbot.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Новые решения первыми (индекс review_post_idx: post_id, created_at DESC).
    List<Review> findAllByTenantIdAndPost_IdOrderByCreatedAtDesc(UUID tenantId, UUID postId);
}