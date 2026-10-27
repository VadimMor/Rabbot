package dev.rabbot.api.services.impl;

import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.ReviewResponse;
import dev.rabbot.api.model.Post;
import dev.rabbot.api.model.Review;
import dev.rabbot.api.enums.ReviewDecision;
import dev.rabbot.api.repository.ReviewRepository;
import dev.rabbot.api.services.PostWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PostWorkflowServiceImpl implements PostWorkflowService {

    private final PostLoader postLoader;
    private final ReviewRepository reviewRepository;

    public PostWorkflowServiceImpl(PostLoader postLoader, ReviewRepository reviewRepository) {
        this.postLoader = postLoader;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public PostResponse submit(UUID tenantId, UUID postId, int version) {
        Post post = postLoader.loadChecked(tenantId, postId, version);
        post.submitForReview();
        // status — поле самой строки post, поэтому @Version вырастет сам.
        return postLoader.saveAndReload(post);
    }

    @Override
    public PostResponse approve(UUID tenantId, UUID postId, int version, String comment) {
        Post post = postLoader.loadChecked(tenantId, postId, version);
        post.approve(Instant.now());
        reviewRepository.save(new Review(post, ReviewDecision.APPROVED, comment));
        return postLoader.saveAndReload(post);
    }

    @Override
    public PostResponse requestChanges(UUID tenantId, UUID postId, int version, String comment) {
        Post post = postLoader.loadChecked(tenantId, postId, version);
        post.requestChanges();
        reviewRepository.save(new Review(post, ReviewDecision.CHANGES_REQUESTED, comment.strip()));
        return postLoader.saveAndReload(post);
    }

    @Override
    public PostResponse archive(UUID tenantId, UUID postId, int version) {
        Post post = postLoader.loadChecked(tenantId, postId, version);
        post.archive();
        return postLoader.saveAndReload(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> reviews(UUID tenantId, UUID postId) {
        // Проверяем, что пост свой: иначе по чужому id вернулся бы пустой список, а не 404.
        postLoader.findWithVariantsOrThrow(tenantId, postId);
        return reviewRepository.findAllByTenantIdAndPost_IdOrderByCreatedAtDesc(tenantId, postId)
                .stream()
                .map(r -> new ReviewResponse(r.getId(), r.getDecision(), r.getComment(),
                        r.getReviewerId(), r.getCreatedAt()))
                .toList();
    }
}