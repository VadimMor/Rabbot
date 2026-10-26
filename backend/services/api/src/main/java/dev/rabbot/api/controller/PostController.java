package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.PostCreateRequest;
import dev.rabbot.api.dto.request.PostUpdateRequest;
import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.enums.PostStatus;
import dev.rabbot.api.services.PostService;
import dev.rabbot.api.utils.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts/{id}")
    public PostResponse get(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return postService.get(tenantId, id);
    }

    // POST /api/v1/brands/{brandId}/posts → 201 + Location: /api/v1/posts/{id}
    @PostMapping("/brands/{brandId}/posts")
    public ResponseEntity<PostResponse> create(@CurrentTenant UUID tenantId,
                                               @PathVariable UUID brandId,
                                               @Valid @RequestBody PostCreateRequest request,
                                               UriComponentsBuilder uriBuilder) {
        PostResponse created = postService.create(tenantId, brandId, request);
        var location = uriBuilder.path("/api/v1/posts/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/posts/{id}")
    public PostResponse update(@CurrentTenant UUID tenantId,
                               @PathVariable UUID id,
                               @Valid @RequestBody PostUpdateRequest request) {
        return postService.update(tenantId, id, request);
    }
}