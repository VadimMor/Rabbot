package dev.rabbot.api.services;

import dev.rabbot.api.dto.request.PostCreateRequest;
import dev.rabbot.api.dto.request.PostUpdateRequest;
import dev.rabbot.api.dto.request.VariantRequest;
import dev.rabbot.api.dto.request.VariantTextRequest;
import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.dto.response.RevisionResponse;
import dev.rabbot.api.enums.PostStatus;

import java.util.UUID;
import java.util.List;

public interface PostService {

    // status = null — без фильтра.
    PageResponse<PostSummaryResponse> listByBrand(UUID tenantId, UUID brandId,
                                                  PostStatus status, int page, int size);

    PostResponse get(UUID tenantId, UUID id);

    PostResponse create(UUID tenantId, UUID brandId, PostCreateRequest request);

    PostResponse update(UUID tenantId, UUID id, PostUpdateRequest request);

    PostResponse addVariant(UUID tenantId, UUID postId, int version, VariantRequest request);

    PostResponse editVariant(UUID tenantId, UUID postId, UUID variantId, int version, VariantTextRequest request);

    PostResponse selectVariant(UUID tenantId, UUID postId, UUID variantId, int version);

    PostResponse deleteVariant(UUID tenantId, UUID postId, UUID variantId, int version);

    List<RevisionResponse> revisions(UUID tenantId, UUID postId, UUID variantId);
}