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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.rabbot.api.enums.SocialPlatform;

// Вариант текста поста под конкретную площадку (таблица core.post_variant).
// На одну площадку может быть несколько вариантов (variant_no 1, 2, 3...),
// но выбран для публикации — не больше одного (частичный уникальный индекс в V6).
@Entity
@Table(name = "post_variant")
public class PostVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    // Владеющая сторона связи: именно здесь колонка post_id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private SocialPlatform platform;

    @Column(name = "variant_no", nullable = false)
    private int variantNo;

    @Column(name = "body", nullable = false)
    private String body = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "hashtags", nullable = false, columnDefinition = "text[]")
    private List<String> hashtags = new ArrayList<>();

    // GENERATED ALWAYS AS (char_length(body)) — считает сама БД.
    // Писать в неё нельзя, Hibernate только перечитывает значение.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "char_count", insertable = false, updatable = false)
    private Integer charCount;

    @Column(name = "is_selected", nullable = false)
    private boolean selected;

    @Column(name = "draft_model")
    private String draftModel;

    @Column(name = "final_model")
    private String finalModel;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected PostVariant() {
    }

    // ---------- геттеры ----------

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public Post getPost() { return post; }
    public SocialPlatform getPlatform() { return platform; }
    public int getVariantNo() { return variantNo; }
    public String getBody() { return body; }
    public List<String> getHashtags() { return hashtags; }
    public Integer getCharCount() { return charCount; }
    public boolean isSelected() { return selected; }
    public String getDraftModel() { return draftModel; }
    public String getFinalModel() { return finalModel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Создаётся только через Post.addVariant(): пост сам решает номер и выбор.
    // Поэтому конструктор package-private (без public).
    PostVariant(Post post, SocialPlatform platform, int variantNo,
                String body, List<String> hashtags, boolean selected) {
        this.tenantId = post.getTenantId();
        this.post = post;
        this.platform = platform;
        this.variantNo = variantNo;
        this.body = body;
        this.hashtags = hashtags == null ? new ArrayList<>() : new ArrayList<>(hashtags);
        this.selected = selected;
    }
}