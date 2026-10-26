package dev.rabbot.api.services;

import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.enums.PostStatus;

import java.util.UUID;

public interface PostService {

    // status = null — без фильтра.
    PageResponse<PostSummaryResponse> listByBrand(UUID tenantId, UUID brandId,
                                                  PostStatus status, int page, int size);

    PostResponse get(UUID tenantId, UUID id);
}