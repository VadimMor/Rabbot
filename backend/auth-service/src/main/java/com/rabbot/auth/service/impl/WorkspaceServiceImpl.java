package com.rabbot.auth.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.rabbot.auth.dto.request.WorkspaceRequest;
import com.rabbot.auth.model.User;
import com.rabbot.auth.model.Workspace;
import com.rabbot.auth.repository.WorkspaceRepository;
import com.rabbot.auth.service.JwtService;
import com.rabbot.auth.service.WorkspaceService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Data
public class WorkspaceServiceImpl implements WorkspaceService {
    private final WorkspaceRepository workspaceRepository;

    private final JwtService jwtService;

    @Override
    public String createWorkspace(WorkspaceRequest workspaceRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Workspace workspace = new Workspace();
        workspace.setName(workspaceRequest.getName());
        workspace.setDescription(workspaceRequest.getDescription());

        workspace.setOwner(currentUser);

        workspaceRepository.save(workspace);
        return "Workspace created successfully";
    }
}
