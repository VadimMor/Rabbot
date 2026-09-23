package com.bluerabbit.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.bluerabbit.content.enums.DraftStatus;

@Entity
@Table(name = "drafts")
@Data
public class Draft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String textContent;

    @Column(name = "target_platforms")
    private String targetPlatforms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}