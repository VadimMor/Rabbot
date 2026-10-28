package dev.rabbot.api.model;

import dev.rabbot.api.enums.RevisionAuthor;
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

// Снимок текста варианта (таблица core.post_revision).
// Только вставка: все колонки updatable = false, сеттеров нет.
@Entity
@Table(name = "post_revision")
public class PostRevision {

    // bigint GENERATED ALWAYS AS IDENTITY: id выдаёт база при INSERT.
    // IDENTITY - Hibernate вставляет строку сразу при save(), не дожидаясь flush.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    // Связь в одну сторону: ревизия → вариант. В PostVariant списка ревизий нет,
    // чтобы история не грузилась вместе с постом.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_variant_id", nullable = false, updatable = false)
    private PostVariant variant;

    @Column(name = "body", nullable = false, updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, updatable = false)
    private RevisionAuthor authorType;

    // Пользователь (когда будет авторизация). Для LLM — null.
    @Column(name = "author_id", updatable = false)
    private UUID authorId;

    // Модель, если автор — LLM.
    @Column(name = "model", updatable = false)
    private String model;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected PostRevision() {
    }

    // Снимок текущего текста варианта, сделанный человеком.
    public static PostRevision byUser(PostVariant variant) {
        PostRevision r = new PostRevision();
        r.tenantId = variant.getTenantId();
        r.variant = variant;
        r.body = variant.getBody();
        r.authorType = RevisionAuthor.USER;
        return r;
    }

    public Long getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public PostVariant getVariant() { return variant; }
    public String getBody() { return body; }
    public RevisionAuthor getAuthorType() { return authorType; }
    public UUID getAuthorId() { return authorId; }
    public String getModel() { return model; }
    public Instant getCreatedAt() { return createdAt; }
}