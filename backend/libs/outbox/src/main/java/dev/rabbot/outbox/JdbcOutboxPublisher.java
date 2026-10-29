package dev.rabbot.outbox;

import dev.rabbot.events.EventHeaders;
import dev.rabbot.events.IntegrationEvent;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Запись события одной строкой INSERT в outbox_event (таблица из V8).
public class JdbcOutboxPublisher implements OutboxPublisher {

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;
    private final String insertSql;

    public JdbcOutboxPublisher(JdbcClient jdbc, JsonMapper json, Clock clock, OutboxProperties properties) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
        // payload и headers — jsonb. CAST(... AS jsonb) вместо "::jsonb".
        this.insertSql = """
                INSERT INTO %s
                    (id, tenant_id, aggregate_type, aggregate_id, event_type,
                     topic, message_key, payload, headers)
                VALUES
                    (:id, :tenantId, :aggregateType, :aggregateId, :eventType,
                     :topic, :messageKey, CAST(:payload AS jsonb), CAST(:headers AS jsonb))
                """.formatted(properties.table());
    }

    // MANDATORY: если активной транзакции нет — исключение.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(IntegrationEvent event) {
        Objects.requireNonNull(event, "event");
        UUID id = UUID.randomUUID();

        // Тело — только данные события (поля record).
        String payload = json.writeValueAsString(event);
        String headers = json.writeValueAsString(headers(id, event));

        jdbc.sql(insertSql)
                .param("id", id)
                .param("tenantId", event.tenantId())
                .param("aggregateType", event.aggregateType())
                .param("aggregateId", event.aggregateId())
                .param("eventType", event.eventType())
                .param("topic", event.topic())
                .param("messageKey", event.key())
                .param("payload", payload)
                .param("headers", headers)
                .update();

        return id;
    }

    // Заголовки Kafka. Всё строками: заголовок в Kafka — это байты, relay запишет каждое значение как UTF-8.
    private Map<String, String> headers(UUID id, IntegrationEvent event) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put(EventHeaders.EVENT_ID, id.toString());
        h.put(EventHeaders.EVENT_TYPE, event.eventType());
        h.put(EventHeaders.SCHEMA_VERSION, String.valueOf(event.schemaVersion()));
        if (event.tenantId() != null) {
            h.put(EventHeaders.TENANT_ID, event.tenantId().toString());
        }
        h.put(EventHeaders.OCCURRED_AT, clock.instant().toString());
        return h;
    }
}