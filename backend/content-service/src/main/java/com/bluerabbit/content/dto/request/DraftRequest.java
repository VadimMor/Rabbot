package com.bluerabbit.content.dto.request;

import com.bluerabbit.content.enums.SocialPlatform;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DraftRequest {
    private String textContent;
    private List<SocialPlatform> targetPlatforms;
    private LocalDateTime scheduledTime;
}