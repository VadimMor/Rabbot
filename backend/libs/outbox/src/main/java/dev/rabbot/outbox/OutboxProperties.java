package dev.rabbot.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Настройки outbox в application.yml сервиса:
//   rabbot:
//     outbox:
//       table: core.outbox_event
@ConfigurationProperties(prefix = "rabbot.outbox")
public record OutboxProperties(
        // Таблица с именем схемы: не зависим от search_path подключения.
        String table
) {
    public OutboxProperties {
        if (table == null || table.isBlank()) {
            table = "core.outbox_event";
        }
        
        // Имя таблицы подставляется в SQL строкой — разрешаем только буквы, цифры, _ и точку.
        if (!table.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?")) {
            throw new IllegalArgumentException("Недопустимое имя таблицы outbox: " + table);
        }
    }
}