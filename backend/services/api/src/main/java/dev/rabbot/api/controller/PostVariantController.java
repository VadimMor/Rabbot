package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.VariantRequest;
import dev.rabbot.api.dto.request.VariantTextRequest;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.services.PostService;
import dev.rabbot.api.utils.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Варианты — вложенный ресурс поста: /posts/{postId}/variants/...
// Каждая операция требует ?version=N — версию поста, которую видел клиент.
// Без неё Spring вернёт 400 (обязательный параметр).
@RestController
@RequestMapping("/api/v1/posts/{postId}/variants")
public class PostVariantController {

    private final PostService postService;

    public PostVariantController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse add(@CurrentTenant UUID tenantId,
                            @PathVariable UUID postId,
                            @RequestParam int version,
                            @Valid @RequestBody VariantRequest request) {
        return postService.addVariant(tenantId, postId, version, request);
    }

    @PutMapping("/{variantId}")
    public PostResponse edit(@CurrentTenant UUID tenantId,
                             @PathVariable UUID postId,
                             @PathVariable UUID variantId,
                             @RequestParam int version,
                             @Valid @RequestBody VariantTextRequest request) {
        return postService.editVariant(tenantId, postId, variantId, version, request);
    }

    @PostMapping("/{variantId}/select")
    public PostResponse select(@CurrentTenant UUID tenantId,
                               @PathVariable UUID postId,
                               @PathVariable UUID variantId,
                               @RequestParam int version) {
        return postService.selectVariant(tenantId, postId, variantId, version);
    }

    // Возвращаем 200 с постом, а не 204: клиенту нужна новая version
    // и, возможно, новый выбранный вариант.
    @DeleteMapping("/{variantId}")
    public PostResponse delete(@CurrentTenant UUID tenantId,
                               @PathVariable UUID postId,
                               @PathVariable UUID variantId,
                               @RequestParam int version) {
        return postService.deleteVariant(tenantId, postId, variantId, version);
    }
}