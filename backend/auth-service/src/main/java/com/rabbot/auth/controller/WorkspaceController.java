package com.rabbot.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Page;

import com.rabbot.auth.Enum.SortEnum;
import com.rabbot.auth.dto.request.WorkspaceRequest;
import com.rabbot.auth.service.WorkspaceService;
import com.rabbot.auth.dto.response.WorkspaceResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    
    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestBody WorkspaceRequest workspaceRequest) {
        return ResponseEntity.ok(workspaceService.createWorkspace(workspaceRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<WorkspaceResponse>> getAllEntity(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "SORT_DATE") SortEnum sort
    ) {
        return ResponseEntity.ok(workspaceService.getAll(page, sort));
    }
}
