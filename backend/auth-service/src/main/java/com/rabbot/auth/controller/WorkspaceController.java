package com.rabbot.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rabbot.auth.dto.request.WorkspaceRequest;
import com.rabbot.auth.service.WorkspaceService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody WorkspaceRequest workspaceRequest) {
        return ResponseEntity.ok(workspaceService.createWorkspace(workspaceRequest));
    }
}
