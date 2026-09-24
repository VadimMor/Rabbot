package com.bluerabbit.content.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class TrendRequest {
    private String title;
    private String content;
    private List<String> sourcePlatform;
    private Double relevanceScore;
}