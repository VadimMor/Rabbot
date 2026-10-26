package dev.rabbot.api.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.rabbot.api.enums.PostKind;
import dev.rabbot.api.enums.PostStatus;

// Пост бренда (таблица core.post). Владеет своими вариантами.
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    // Много постов → один бренд.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false, updatable = false)
    private Brand brand;

    // Тренд, из которого родился пост. Сущности Trend в api пока нет,
    // поэтому храним просто id.
    @Column(name = "trend_id")
    private UUID trendId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false)
    private PostKind kind;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PostStatus status;

    // Оценка LLM-судьи 0–100 (smallint в БД).
    @Column(name = "brand_score")
    private Integer brandScore;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    // Оптимистичная блокировка: Hibernate добавляет "WHERE version = ?" в UPDATE
    // и увеличивает число. Если двое редактируют пост одновременно,
    // второй получит ошибку вместо тихой перезаписи чужих правок.
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    // Один пост → много вариантов.
    // mappedBy = "post": связь хранится в поле PostVariant.post (колонка post_id),
    //   здесь — только "обратная сторона" для удобного доступа.
    // cascade = ALL: сохранение/удаление поста сохраняет/удаляет варианты.
    // orphanRemoval: вариант, убранный из списка, удаляется из БД.
    // @OrderBy: порядок при загрузке — по площадке, затем по номеру варианта.
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("platform ASC, variantNo ASC")
    private List<PostVariant> variants = new ArrayList<>();

    protected Post() {
    }

    // ---------- геттеры ----------

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public Brand getBrand() { return brand; }
    public UUID getTrendId() { return trendId; }
    public PostKind getKind() { return kind; }
    public String getTitle() { return title; }
    public PostStatus getStatus() { return status; }
    public Integer getBrandScore() { return brandScore; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PostVariant> getVariants() { return variants; }
}