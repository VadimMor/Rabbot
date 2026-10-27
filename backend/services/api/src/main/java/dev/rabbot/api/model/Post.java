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
import dev.rabbot.api.utils.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.Map;

import dev.rabbot.api.enums.PostKind;
import dev.rabbot.api.enums.PostStatus;
import dev.rabbot.api.utils.ConflictException;

import dev.rabbot.api.enums.SocialPlatform;

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

    // Разрешённые переходы статусов, которые делает пользователь через API.
    // Статусов, которых нет в ключах (GENERATING, SCHEDULED, ARCHIVED),
    // пользователь покинуть не может — их меняют другие сервисы или они конечные.
    private static final Map<PostStatus, Set<PostStatus>> TRANSITIONS = Map.of(
            PostStatus.DRAFT, EnumSet.of(PostStatus.IN_REVIEW, PostStatus.ARCHIVED),
            PostStatus.IN_REVIEW, EnumSet.of(PostStatus.APPROVED, PostStatus.CHANGES_REQUESTED, PostStatus.ARCHIVED),
            PostStatus.CHANGES_REQUESTED, EnumSet.of(PostStatus.IN_REVIEW, PostStatus.ARCHIVED),
            PostStatus.APPROVED, EnumSet.of(PostStatus.ARCHIVED),
            PostStatus.PUBLISHED, EnumSet.of(PostStatus.ARCHIVED),
            PostStatus.PARTIALLY_PUBLISHED, EnumSet.of(PostStatus.ARCHIVED),
            PostStatus.FAILED, EnumSet.of(PostStatus.ARCHIVED)
    );

    // В каких статусах пост ещё можно редактировать.
    // После одобрения и публикации текст менять нельзя.
    private static final Set<PostStatus> EDITABLE =
            EnumSet.of(PostStatus.DRAFT, PostStatus.IN_REVIEW, PostStatus.CHANGES_REQUESTED);

        // Новый пост — всегда черновик.
    public Post(Brand brand, PostKind kind, String title, UUID trendId) {
        this.tenantId = brand.getTenantId();
        this.brand = brand;
        this.kind = kind;
        this.title = title;
        this.trendId = trendId;
        this.status = PostStatus.DRAFT;
    }

    // ---------- бизнес-методы ----------

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    // Бросает 409, если пост нельзя менять в текущем статусе.
    public void requireEditable() {
        if (!isEditable()) {
            throw new ConflictException("Пост в статусе " + status + " редактировать нельзя");
        }
    }

    public void rename(String newTitle) {
        requireEditable();
        this.title = newTitle;
    }

    // Добавить вариант. Номер считается сам: следующий для этой площадки.
    // Первый вариант площадки сразу выбирается для публикации.
    public PostVariant addVariant(SocialPlatform platform, String body, List<String> hashtags) {
        requireEditable();

        int nextNo = variants.stream()
                .filter(v -> v.getPlatform() == platform)
                .mapToInt(PostVariant::getVariantNo)
                .max()
                .orElse(0) + 1;

        boolean hasSelected = variants.stream()
                .anyMatch(v -> v.getPlatform() == platform && v.isSelected());

        PostVariant variant = new PostVariant(this, platform, nextNo, body, hashtags, !hasSelected);
        // Добавляем в список: благодаря cascade = ALL вариант сохранится вместе с постом.
        variants.add(variant);
        return variant;
    }
    
        // Найти вариант этого поста. Чужой id (даже существующий) → 404.
    public PostVariant findVariant(UUID variantId) {
        return variants.stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Вариант " + variantId + " не найден"));
    }

    public void editVariant(PostVariant variant, String body, List<String> hashtags) {
        requireEditable();
        variant.edit(body, hashtags);
    }

    // Снять выбор со всех вариантов площадки (шаг 1 из 2 при смене выбора).
    public void unselectPlatform(SocialPlatform platform) {
        requireEditable();
        variants.stream()
                .filter(v -> v.getPlatform() == platform)
                .forEach(PostVariant::unselect);
    }

    // Выбрать вариант (шаг 2 из 2). Вызывать после unselectPlatform + flush.
    public void selectVariant(PostVariant variant) {
        requireEditable();
        variant.select();
    }

    // Удалить вариант. orphanRemoval = true → Hibernate сам сделает DELETE.
    public void removeVariant(PostVariant variant) {
        requireEditable();
        variants.remove(variant);
    }

    // Вариант площадки с наименьшим номером — кандидат на выбор после удаления выбранного.
    public java.util.Optional<PostVariant> firstVariantOf(SocialPlatform platform) {
        return variants.stream()
                .filter(v -> v.getPlatform() == platform)
                .min(java.util.Comparator.comparingInt(PostVariant::getVariantNo));
    }

    // ---------- жизненный цикл ----------
    // Отправить на ревью. Пустой пост ревьюеру не нужен:
    // должен быть хотя бы один выбранный вариант с текстом.
    public void submitForReview() {
        boolean hasPublishable = variants.stream()
                .anyMatch(v -> v.isSelected() && !v.getBody().isBlank());
        if (!hasPublishable) {
            throw new ConflictException("Нет выбранного варианта с текстом — отправлять на ревью нечего");
        }
        moveTo(PostStatus.IN_REVIEW);
    }

    public void approve(Instant now) {
        moveTo(PostStatus.APPROVED);
        this.approvedAt = now;
        // approvedBy заполним, когда появится авторизация (Keycloak).
    }

    public void requestChanges() {
        moveTo(PostStatus.CHANGES_REQUESTED);
    }

    public void archive() {
        moveTo(PostStatus.ARCHIVED);
    }

    // Единственное место, где меняется status: здесь проверяется таблица переходов.
    private void moveTo(PostStatus target) {
        Set<PostStatus> allowed = TRANSITIONS.getOrDefault(status, Set.of());
        if (!allowed.contains(target)) {
            throw new ConflictException("Нельзя перевести пост из " + status + " в " + target);
        }
        this.status = target;
    }
}