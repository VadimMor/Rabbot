package com.bluerabbit.content.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DraftRequest {
    private String textContent;
    private List<String> targetPlatforms;
    private LocalDateTime scheduledTime;
}