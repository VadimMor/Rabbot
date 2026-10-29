package dev.rabbot.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Настройки в application.yml сервиса:
//   rabbot:
//     outbox:
//       table: core.outbox_event
//       relay:
//         enabled: true
//         poll-interval: 500ms
//         batch-size: 100
//         max-attempts: 10
//         send-timeout: 10s
@ConfigurationProperties(prefix = "rabbot.outbox")
public record OutboxProperties(String table, Relay relay) {

    public OutboxProperties {
        if (table == null || table.isBlank()) {
            table = "core.outbox_event";
        }
        if (!table.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?")) {
            throw new IllegalArgumentException("Недопустимое имя таблицы outbox: " + table);
        }
        if (relay == null) {
            relay = new Relay(false, null, 0, 0, null);
        }
    }

    public record Relay(
            // Включить фоновую отправку в Kafka.
            boolean enabled,
            // Пауза между проходами.
            Duration pollInterval,
            // Сколько строк брать за один проход (и держать заблокированными).
            int batchSize,
            // После стольких неудач событие больше не отправляется (см. last_error).
            int maxAttempts,
            // Сколько ждать подтверждения от Kafka на одно сообщение.
            Duration sendTimeout
    ) {
        public Relay {
            if (pollInterval == null) pollInterval = Duration.ofMillis(500);
            if (batchSize <= 0) batchSize = 100;
            if (maxAttempts <= 0) maxAttempts = 10;
            if (sendTimeout == null) sendTimeout = Duration.ofSeconds(10);
        }
    }
}