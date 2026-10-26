package dev.rabbot.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

import dev.rabbot.api.enums.ChannelStatus;
import dev.rabbot.api.enums.SocialPlatform;

// Подключённый аккаунт соцсети (таблица core.channel).
// Создаётся OAuth-подключением (будет позже), поэтому здесь нет публичного
// конструктора: API только читает канал и меняет его настройки.
@Entity
@Table(name = "channel")
public class Channel {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    // Связь "много каналов → один бренд".
    // LAZY: бренд не грузится из БД, пока к нему не обратятся.
    // Для brand.getId() Hibernate запрос не делает: id уже известен из brand_id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false, updatable = false)
    private Brand brand;

    // Enum хранится в БД строкой ('TELEGRAM', 'VK', ...).
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, updatable = false)
    private SocialPlatform platform;

    // id аккаунта в самой соцсети (например, id Telegram-канала).
    @Column(name = "external_account_id", nullable = false, updatable = false)
    private String externalAccountId;

    @Column(name = "handle")
    private String handle;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChannelStatus status;

    @Column(name = "auto_publish", nullable = false)
    private boolean autoPublish;

    @Column(name = "daily_post_limit", nullable = false)
    private int dailyPostLimit;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    // Путь к OAuth-токену в Vault. Сам токен в БД не хранится
    // и наружу через API не отдаётся.
    @Column(name = "secret_ref")
    private String secretRef;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scopes", nullable = false, columnDefinition = "text[]")
    private List<String> scopes = new ArrayList<>();

    @Column(name = "audience_size")
    private Integer audienceSize;

    // Кто подключил канал. Сущности пользователя пока нет, поэтому просто UUID.
    @Column(name = "connected_by", updatable = false)
    private UUID connectedBy;

    @Column(name = "connected_at", insertable = false, updatable = false)
    private Instant connectedAt;

    @Column(name = "last_error")
    private String lastError;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // Обновляется триггером channel_set_updated_at.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Channel() {
    }

    // ---------- бизнес-методы ----------

    // Отключить канал: публикации в него больше не уходят.
    // Отзыв токена в Vault добавим вместе с OAuth.
    public void disconnect() {
        this.status = ChannelStatus.DISCONNECTED;
        this.autoPublish = false;
    }

    // ---------- геттеры ----------

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public Brand getBrand() { return brand; }
    public SocialPlatform getPlatform() { return platform; }
    public String getExternalAccountId() { return externalAccountId; }
    public String getHandle() { return handle; }
    public String getDisplayName() { return displayName; }
    public ChannelStatus getStatus() { return status; }
    public boolean isAutoPublish() { return autoPublish; }
    public int getDailyPostLimit() { return dailyPostLimit; }
    public String getTimezone() { return timezone; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public List<String> getScopes() { return scopes; }
    public Integer getAudienceSize() { return audienceSize; }
    public UUID getConnectedBy() { return connectedBy; }
    public Instant getConnectedAt() { return connectedAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---------- сеттеры только для настроек ----------

    public void setAutoPublish(boolean autoPublish) { this.autoPublish = autoPublish; }
    public void setDailyPostLimit(int dailyPostLimit) { this.dailyPostLimit = dailyPostLimit; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}