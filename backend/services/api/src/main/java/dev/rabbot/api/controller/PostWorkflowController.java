package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.RequestChangesRequest;
import dev.rabbot.api.dto.request.ReviewRequest;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.ReviewResponse;
import dev.rabbot.api.services.PostWorkflowService;
import dev.rabbot.api.utils.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Действия над постом — POST на путь с глаголом: /posts/{id}/approve и т. д.
// Так клиент не может "просто записать" любой статус через PATCH,
// а сервер проверяет каждый переход.
@RestController
@RequestMapping("/api/v1/posts/{postId}")
public class PostWorkflowController {

    private final PostWorkflowService workflowService;

    public PostWorkflowController(PostWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/submit")
    public PostResponse submit(@CurrentTenant UUID tenantId,
                               @PathVariable UUID postId,
                               @RequestParam int version) {
        return workflowService.submit(tenantId, postId, version);
    }

    // Тело необязательно: одобрить можно и без комментария.
    @PostMapping("/approve")
    public PostResponse approve(@CurrentTenant UUID tenantId,
                                @PathVariable UUID postId,
                                @RequestParam int version,
                                @Valid @RequestBody(required = false) ReviewRequest request) {
        String comment = request == null ? null : request.comment();
        return workflowService.approve(tenantId, postId, version, comment);
    }

    @PostMapping("/request-changes")
    public PostResponse requestChanges(@CurrentTenant UUID tenantId,
                                       @PathVariable UUID postId,
                                       @RequestParam int version,
                                       @Valid @RequestBody RequestChangesRequest request) {
        return workflowService.requestChanges(tenantId, postId, version, request.comment());
    }

    @PostMapping("/archive")
    public PostResponse archive(@CurrentTenant UUID tenantId,
                                @PathVariable UUID postId,
                                @RequestParam int version) {
        return workflowService.archive(tenantId, postId, version);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews(@CurrentTenant UUID tenantId, @PathVariable UUID postId) {
        return workflowService.reviews(tenantId, postId);
    }
}