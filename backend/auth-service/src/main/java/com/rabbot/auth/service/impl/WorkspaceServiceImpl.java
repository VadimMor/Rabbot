package com.rabbot.auth.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.rabbot.auth.Enum.SortEnum;
import com.rabbot.auth.dto.request.WorkspaceRequest;
import com.rabbot.auth.dto.response.WorkspaceResponse;
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

    @Override
    public Page<WorkspaceResponse> getAll(Integer page, SortEnum sort) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        Sort springSort = (sort == SortEnum.SORT_NAME) 
                ? Sort.by(Sort.Direction.ASC, "name") 
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(page, 10, springSort);
        
        Page<Workspace> workspacesPage = workspaceRepository.findAllByOwnerId(
            currentUser.getId(),
            pageable
        );
        
        return workspacesPage.map(workspace -> WorkspaceResponse.builder()
            .id(workspace.getId())
            .name(workspace.getName())
            .description(workspace.getDescription())
            .createdAt(workspace.getCreatedAt())
            .build()
        );
    }
}
