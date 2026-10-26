package dev.rabbot.api.utils;

import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.dto.response.PostVariantResponse;
import dev.rabbot.api.model.Post;
import dev.rabbot.api.model.PostVariant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {

    public PostSummaryResponse toSummary(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getBrand().getId(),
                post.getTrendId(),
                post.getKind(),
                post.getTitle(),
                post.getStatus(),
                post.getBrandScore(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public PostResponse toResponse(Post post) {
        List<PostVariantResponse> variants = post.getVariants().stream()
                .map(this::toVariantResponse)
                .toList();

        return new PostResponse(
                post.getId(),
                post.getBrand().getId(),
                post.getTrendId(),
                post.getKind(),
                post.getTitle(),
                post.getStatus(),
                post.getBrandScore(),
                post.getCreatedBy(),
                post.getApprovedBy(),
                post.getApprovedAt(),
                post.getVersion(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                variants
        );
    }

    public PostVariantResponse toVariantResponse(PostVariant variant) {
        return new PostVariantResponse(
                variant.getId(),
                variant.getPlatform(),
                variant.getVariantNo(),
                variant.getBody(),
                List.copyOf(variant.getHashtags()),
                variant.getCharCount(),
                variant.isSelected(),
                variant.getDraftModel(),
                variant.getFinalModel(),
                variant.getUpdatedAt()
        );
    }
}