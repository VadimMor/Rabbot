package dev.rabbot.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// "Пора опубликовать". Отправляет scheduler, читает publisher.
// Топик publish.due.
public record PublishDue(
        UUID publicationId,
        UUID tenantId,
        UUID channelId,
        UUID postVariantId,
        // Площадка строкой ("TELEGRAM", "VK", ...), как DOMAIN social_platform в БД.
        String platform,
        Instant scheduledAt,
        int attempt
) implements IntegrationEvent {

    public static final String TYPE = "PublishDue";
    public static final int VERSION = 1;

    // Компактный конструктор: проверка при создании.
    // Сломанное событие не должно уйти в Kafka.
    public PublishDue {
        Objects.requireNonNull(publicationId, "publicationId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(postVariantId, "postVariantId");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(scheduledAt, "scheduledAt");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt должен быть >= 1, а не " + attempt);
        }
    }

    @Override public String topic() { return Topics.PUBLISH_DUE; }
    @Override public String key() { return channelId.toString(); }
    @Override public String eventType() { return TYPE; }
    @Override public int schemaVersion() { return VERSION; }
    @Override public String aggregateType() { return "Publication"; }
    @Override public String aggregateId() { return publicationId.toString(); }
}