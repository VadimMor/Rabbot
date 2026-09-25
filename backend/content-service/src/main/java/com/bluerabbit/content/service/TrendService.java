package com.bluerabbit.content.service;

import com.bluerabbit.content.dto.request.TrendRequest;
import com.bluerabbit.content.dto.response.TrendResponse;

import java.util.List;

public interface TrendService {
    TrendResponse createTrend(Long workspaceId, TrendRequest request);
    List<TrendResponse> getAllTrendsByWorkspace(Long workspaceId);
}