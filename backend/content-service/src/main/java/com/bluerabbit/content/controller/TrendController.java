package com.bluerabbit.content.controller;

import com.bluerabbit.content.dto.request.TrendRequest;
import com.bluerabbit.content.dto.response.TrendResponse;
import com.bluerabbit.content.service.TrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content/{workspaceId}")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    @PostMapping("/trends")
    public ResponseEntity<TrendResponse> createTrend(
            @PathVariable Long workspaceId,
            @RequestBody TrendRequest request) {
        return ResponseEntity.ok(trendService.createTrend(workspaceId, request));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<TrendResponse>> getTrends(
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok(trendService.getAllTrendsByWorkspace(workspaceId));
    }
}