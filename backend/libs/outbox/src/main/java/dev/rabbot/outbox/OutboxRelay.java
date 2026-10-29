package dev.rabbot.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// Один проход relay: взять пачку событий из outbox и отправить в Kafka.
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final TypeReference<Map<String, String>> HEADERS_TYPE = new TypeReference<>() {
    };

    // Строка outbox, нужная для отправки.
    private record Row(UUID id, String topic, String key, String payload, String headers) {
    }

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;
    private final JsonMapper json;
    private final OutboxProperties.Relay config;

    private final String selectSql;
    private final String publishedSql;
    private final String failedSql;

    public OutboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate tx,
                       JsonMapper json, OutboxProperties properties) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = tx;
        this.json = json;
        this.config = properties.relay();
        String table = properties.table();

        // FOR UPDATE SKIP LOCKED: строки, которые уже взял другой экземпляр relay,
        // пропускаются, а не ждут. Так несколько экземпляров делят работу без дублей.
        this.selectSql = """
                SELECT id, topic, message_key,
                       CAST(payload AS text) AS payload,
                       CAST(headers AS text) AS headers
                FROM %s
                WHERE published_at IS NULL AND attempts < :maxAttempts
                ORDER BY created_at, id
                LIMIT :batch
                FOR UPDATE SKIP LOCKED
                """.formatted(table);
        this.publishedSql = "UPDATE %s SET published_at = now(), last_error = NULL WHERE id = :id"
                .formatted(table);
        this.failedSql = "UPDATE %s SET attempts = attempts + 1, last_error = left(:error, 2000) WHERE id = :id"
                .formatted(table);
    }

    // Отправить одну пачку. Возвращает число успешно отправленных событий.
    public int relayBatch() {
        Integer sent = tx.execute(status -> relayLocked());
        return sent == null ? 0 : sent;
    }

    private int relayLocked() {
        List<Row> rows = jdbc.sql(selectSql)
                .param("maxAttempts", config.maxAttempts())
                .param("batch", config.batchSize())
                .query((rs, n) -> new Row(
                        rs.getObject("id", UUID.class),
                        rs.getString("topic"),
                        rs.getString("message_key"),
                        rs.getString("payload"),
                        rs.getString("headers")))
                .list();

        // Ключи, по которым в этой пачке уже была ошибка: следующие события
        // с тем же ключом не отправляем, чтобы не нарушить их порядок.
        Set<String> blockedKeys = new HashSet<>();
        int sent = 0;

        for (Row row : rows) {
            if (row.key() != null && blockedKeys.contains(row.key())) {
                continue;
            }
            try {
                // Ждём подтверждения брокера: published_at ставим только
                // за то, что Kafka точно приняла.
                kafka.send(toRecord(row)).get(config.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
                jdbc.sql(publishedSql).param("id", row.id()).update();
                sent++;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Throwable cause = (e instanceof ExecutionException && e.getCause() != null) ? e.getCause() : e;
                String error = cause.getClass().getSimpleName() + ": " + cause.getMessage();
                jdbc.sql(failedSql).param("id", row.id()).param("error", error).update();
                if (row.key() != null) {
                    blockedKeys.add(row.key());
                }
                log.warn("Outbox: не удалось отправить событие {} в {}: {}", row.id(), row.topic(), error);
            }
        }
        return sent;
    }

    // Строка outbox → сообщение Kafka: ключ, тело-JSON, заголовки байтами UTF-8.
    private ProducerRecord<String, String> toRecord(Row row) {
        var record = new ProducerRecord<String, String>(row.topic(), null, row.key(), row.payload());
        Map<String, String> headers = json.readValue(row.headers(), HEADERS_TYPE);
        headers.forEach((name, value) -> record.headers().add(name, value.getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}