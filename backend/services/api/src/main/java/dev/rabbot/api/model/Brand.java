package dev.rabbot.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Сущность = строка таблицы core.brand (схему задаёт default_schema в application.yml).
@Entity
@Table(name = "brand")
public class Brand {

    // Первичный ключ. UUID генерирует Hibernate перед вставкой.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Организация-владелец. После создания не меняется (updatable = false).
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "audience")
    private String audience;

    @Column(name = "language", nullable = false)
    private String language;

    // Колонка text[] в PostgreSQL отображается в List<String>.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tone", nullable = false, columnDefinition = "text[]")
    private List<String> tone = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "forbidden_words", nullable = false, columnDefinition = "text[]")
    private List<String> forbiddenWords = new ArrayList<>();

    @Column(name = "default_cta")
    private String defaultCta;

    // jsonb храним в Java как строку с JSON.
    // ColumnTransformer дописывает "::jsonb" при записи, иначе PostgreSQL
    // откажется класть text в колонку jsonb.
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "style_rules", nullable = false, columnDefinition = "jsonb")
    private String styleRules = "{}";

    // created_at заполняет БД (DEFAULT now()). Hibernate не пишет это поле,
    // а после INSERT перечитывает его из базы.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // updated_at обновляет триггер brand_set_updated_at.
    // Hibernate перечитывает поле после INSERT и UPDATE.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    // Пустой конструктор обязателен для JPA. protected — чтобы его не вызывали в коде.
    protected Brand() {
    }

    // Новый бренд всегда создаётся сразу с организацией.
    public Brand(UUID tenantId) {
        this.tenantId = tenantId;
    }

    // ---------- геттеры и сеттеры ----------

    public UUID getId() { return id; }

    public UUID getTenantId() { return tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<String> getTone() { return tone; }
    public void setTone(List<String> tone) { this.tone = tone; }

    public List<String> getForbiddenWords() { return forbiddenWords; }
    public void setForbiddenWords(List<String> forbiddenWords) { this.forbiddenWords = forbiddenWords; }

    public String getDefaultCta() { return defaultCta; }
    public void setDefaultCta(String defaultCta) { this.defaultCta = defaultCta; }

    public String getStyleRules() { return styleRules; }
    public void setStyleRules(String styleRules) { this.styleRules = styleRules; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}