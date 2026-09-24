package com.bluerabbit.content.dto.response;

import com.bluerabbit.content.enums.SocialPlatform;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DraftResponse {
    private Long id;
    private String textContent;
    private List<String> targetPlatforms;
    private String status;
    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt;
}