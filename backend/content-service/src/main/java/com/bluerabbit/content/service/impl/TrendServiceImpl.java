package com.bluerabbit.content.service.impl;

import com.bluerabbit.content.dto.request.TrendRequest;
import com.bluerabbit.content.dto.response.TrendResponse;
import com.bluerabbit.content.model.Trend;
import com.bluerabbit.content.repository.TrendRepository;
import com.bluerabbit.content.service.TrendService;
import com.bluerabbit.content.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrendServiceImpl implements TrendService {

    private final TrendRepository trendRepository;

    @Override
    @Transactional
    public TrendResponse createTrend(Long workspaceId, TrendRequest request) {
        Trend trend = new Trend();
        trend.setWorkspaceId(workspaceId);
        trend.setTitle(request.getTitle());
        trend.setContent(request.getContent());
        
        // Обработка списка платформ
        if (request.getSourcePlatform() != null) {
            trend.setTargetPlatforms(request.getSourcePlatform().stream()
                    .map(SocialPlatform::valueOf)
                    .collect(Collectors.toList()));
        }
        
        trend.setRelevanceScore(request.getRelevanceScore());

        Trend savedTrend = trendRepository.save(trend);

        return mapToResponse(savedTrend);
    }

    @Override
    public List<TrendResponse> getAllTrendsByWorkspace(Long workspaceId) {
        return trendRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TrendResponse mapToResponse(Trend trend) {
        return TrendResponse.builder()
                .id(trend.getId())
                .title(trend.getTitle())
                .content(trend.getContent())
                // Преобразование обратно в список строк для ответа
                .sourcePlatforms(trend.getTargetPlatforms() != null ? 
                        trend.getTargetPlatforms().stream()
                                .map(SocialPlatform::name)
                                .collect(Collectors.toList()) 
                        : null)
                .relevanceScore(trend.getRelevanceScore())
                .createdAt(trend.getCreatedAt())
                .build();
    }
}