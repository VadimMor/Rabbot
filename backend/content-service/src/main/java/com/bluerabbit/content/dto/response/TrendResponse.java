package com.bluerabbit.content.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrendResponse {
    private Long id;
    private String title;
    private String content;
    private List<String> sourcePlatforms;
    private Double relevanceScore;
    private LocalDateTime createdAt;
}