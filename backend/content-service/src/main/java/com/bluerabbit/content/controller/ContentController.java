package com.bluerabbit.content.controller;

import com.bluerabbit.content.dto.request.DraftRequest;
import com.bluerabbit.content.dto.response.DraftResponse;
import com.bluerabbit.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content/{workspaceId}")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping("/drafts")
    public ResponseEntity<DraftResponse> createDraft(
            @PathVariable Long workspaceId,
            @RequestBody DraftRequest request) {
        return ResponseEntity.ok(contentService.createDraft(workspaceId, request));
    }

    @GetMapping("/drafts")
    public ResponseEntity<List<DraftResponse>> getDrafts(
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok(contentService.getAllDraftsByWorkspace(workspaceId));
    }
}