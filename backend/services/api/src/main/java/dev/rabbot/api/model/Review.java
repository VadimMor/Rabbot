package dev.rabbot.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

import dev.rabbot.api.enums.ReviewDecision;

// Решение ревьюера по посту (таблица core.review).
// @Immutable: запись истории не меняется после создания,
// Hibernate даже не будет проверять её на изменения.
@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    // Связь только в одну сторону: review → post.
    // В Post списка ревью нет — он не нужен при каждой загрузке поста.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    // Кто решил. null — пока нет авторизации (или решение LLM-судьи).
    @Column(name = "reviewer_id", updatable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, updatable = false)
    private ReviewDecision decision;

    @Column(name = "comment", updatable = false)
    private String comment;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Review() {
    }

    public Review(Post post, ReviewDecision decision, String comment) {
        this.tenantId = post.getTenantId();
        this.post = post;
        this.decision = decision;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public Post getPost() { return post; }
    public UUID getReviewerId() { return reviewerId; }
    public ReviewDecision getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}