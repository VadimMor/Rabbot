package com.bluerabbit.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

import com.bluerabbit.content.enums.SocialPlatform;


@Entity
@Table(name = "trends")
@Data
public class Trend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId; // Жесткая привязка к проекту

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "target_platforms", columnDefinition = "text[]")
    private List<SocialPlatform> targetPlatforms; // Список платформ, на которых актуальна данная тенденция

    @Column(name = "relevance_score")
    private Double relevanceScore; // Оценка AI от 0 до 1

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}