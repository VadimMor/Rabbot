package dev.rabbot.api.controller;

import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.enums.PostStatus;
import dev.rabbot.api.services.PostService;
import dev.rabbot.api.utils.CurrentTenant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // GET /api/v1/brands/{brandId}/posts?status=IN_REVIEW&page=0&size=20
    // Строку "IN_REVIEW" Spring сам превратит в enum; неизвестное значение → 400.
    @GetMapping("/brands/{brandId}/posts")
    public PageResponse<PostSummaryResponse> listByBrand(
            @CurrentTenant UUID tenantId,
            @PathVariable UUID brandId,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return postService.listByBrand(tenantId, brandId, status, page, size);
    }

    @GetMapping("/posts/{id}")
    public PostResponse get(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return postService.get(tenantId, id);
    }
}