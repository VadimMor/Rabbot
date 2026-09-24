package com.bluerabbit.content.service.impl;

import com.bluerabbit.content.dto.request.DraftRequest;
import com.bluerabbit.content.dto.response.DraftResponse;
import com.bluerabbit.content.model.Draft;
import com.bluerabbit.content.repository.DraftRepository;
import com.bluerabbit.content.service.ContentService;
import com.bluerabbit.content.enums.DraftStatus;
import com.bluerabbit.content.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final DraftRepository draftRepository;

    @Override
    @Transactional
    public DraftResponse createDraft(Long workspaceId, DraftRequest request) {
        Draft draft = new Draft();
        draft.setWorkspaceId(workspaceId);
        draft.setTextContent(request.getTextContent());
        draft.setTargetPlatforms(request.getTargetPlatforms().stream()
                .map(SocialPlatform::valueOf)
                .collect(Collectors.toList()));
        draft.setStatus(DraftStatus.DRAFT);
        draft.setScheduledTime(request.getScheduledTime());

        Draft savedDraft = draftRepository.save(draft);

        return mapToResponse(savedDraft);
    }

    @Override
    public List<DraftResponse> getAllDraftsByWorkspace(Long workspaceId) {
        return draftRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DraftResponse mapToResponse(Draft draft) {
        return DraftResponse.builder()
                .id(draft.getId())
                .textContent(draft.getTextContent())
                .targetPlatforms(draft.getTargetPlatforms().stream()
                        .map(SocialPlatform::name)
                        .collect(Collectors.toList())
                    )
                .status(draft.getStatus().toString())
                .scheduledTime(draft.getScheduledTime())
                .createdAt(draft.getCreatedAt())
                .build();
    }
}