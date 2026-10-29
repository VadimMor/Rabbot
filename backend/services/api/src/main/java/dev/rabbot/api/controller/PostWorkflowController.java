package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.RequestChangesRequest;
import dev.rabbot.api.dto.request.ReviewRequest;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.ReviewResponse;
import dev.rabbot.api.services.PostWorkflowService;
import dev.rabbot.api.utils.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ревью постов", description = "Жизненный цикл: черновик → ревью → одобрен / на доработку → архив")
public class PostWorkflowController {

    private final PostWorkflowService workflowService;

    public PostWorkflowController(PostWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/submit")
    @Operation(summary = "Отправить на ревью", description = "Нужен хотя бы один выбранный вариант с текстом")
    public PostResponse submit(@CurrentTenant UUID tenantId,
                               @PathVariable UUID postId,
                               @RequestParam int version) {
        return workflowService.submit(tenantId, postId, version);
    }

    // Тело необязательно: одобрить можно и без комментария.
    @PostMapping("/approve")
    @Operation(summary = "Одобрить", description = "Комментарий необязателен. После одобрения текст не редактируется")
    public PostResponse approve(@CurrentTenant UUID tenantId,
                                @PathVariable UUID postId,
                                @RequestParam int version,
                                @Valid @RequestBody(required = false) ReviewRequest request) {
        String comment = request == null ? null : request.comment();
        return workflowService.approve(tenantId, postId, version, comment);
    }

    @PostMapping("/request-changes")
    @Operation(summary = "Вернуть на доработку", description = "Комментарий обязателен")
    public PostResponse requestChanges(@CurrentTenant UUID tenantId,
                                       @PathVariable UUID postId,
                                       @RequestParam int version,
                                       @Valid @RequestBody RequestChangesRequest request) {
        return workflowService.requestChanges(tenantId, postId, version, request.comment());
    }

    @PostMapping("/archive")
    @Operation(summary = "В архив")
    public PostResponse archive(@CurrentTenant UUID tenantId,
                                @PathVariable UUID postId,
                                @RequestParam int version) {
        return workflowService.archive(tenantId, postId, version);
    }

    @GetMapping("/reviews")
    @Operation(summary = "История ревью", description = "Новые решения первыми")
    public List<ReviewResponse> reviews(@CurrentTenant UUID tenantId, @PathVariable UUID postId) {
        return workflowService.reviews(tenantId, postId);
    }
}