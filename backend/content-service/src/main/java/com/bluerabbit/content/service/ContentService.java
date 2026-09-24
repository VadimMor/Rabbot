package com.bluerabbit.content.service;

import com.bluerabbit.content.dto.request.DraftRequest;
import com.bluerabbit.content.dto.response.DraftResponse;

import java.util.List;

public interface ContentService {
    DraftResponse createDraft(Long workspaceId, DraftRequest request);

    List<DraftResponse> getAllDraftsByWorkspace(Long workspaceId);
}