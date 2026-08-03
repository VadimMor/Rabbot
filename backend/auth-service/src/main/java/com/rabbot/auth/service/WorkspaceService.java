package com.rabbot.auth.service;

import org.springframework.data.domain.Page;

import com.rabbot.auth.Enum.SortEnum;
import com.rabbot.auth.dto.request.WorkspaceRequest;
import com.rabbot.auth.dto.response.WorkspaceResponse;

import com.rabbot.auth.model.User;
public interface WorkspaceService {
    String createWorkspace(WorkspaceRequest workspaceRequest);

    Page<WorkspaceResponse> getAll(Integer page, SortEnum sort);
    
    void warmUpUserCache(User user);
}
