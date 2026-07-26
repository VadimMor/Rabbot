package com.rabbot.auth.service;

import com.rabbot.auth.dto.request.WorkspaceRequest;

public interface WorkspaceService {
    String createWorkspace(WorkspaceRequest workspaceRequest);
}
