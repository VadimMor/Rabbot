package dev.rabbot.outbox;

import dev.rabbot.events.PublishDue;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxTestApplication.class)
@Import(OutboxTestApplication.Containers.class)
class OutboxRelayIT {

    @Autowired OutboxPublisher outbox;
    @Autowired OutboxRelay relay;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    @Autowired ConsumerFactory<String, String> consumerFactory;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE core.outbox_event");
    }

    private static PublishDue sampleEvent() {
        return new PublishDue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "TELEGRAM", Instant.parse("2026-10-29T09:00:00Z"), 1);
    }

    // Прочитать из топика сообщение с нужным ключом (новая группа — читаем с начала).
    private ConsumerRecord<String, String> receive(String topic, String key) {
        try (Consumer<String, String> consumer =
                     consumerFactory.createConsumer("it-" + UUID.randomUUID(), null)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("Сообщение с ключом " + key + " не пришло в " + topic);
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    @Test
    void relaysEventToKafkaWithKeyPayloadAndHeaders() {
        PublishDue event = sampleEvent();
        UUID eventId = tx.execute(status -> outbox.publish(event));

        assertThat(relay.relayBatch()).isEqualTo(1);

        // В БД отмечено как отправленное.
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT published_at, attempts, last_error FROM core.outbox_event WHERE id = ?", eventId);
        assertThat(row.get("published_at")).isNotNull();
        assertThat(row.get("attempts")).isEqualTo(0);

        // В Kafka: тот же топик, ключ = канал, тело — JSON события, заголовки.
        ConsumerRecord<String, String> record = receive("publish.due", event.channelId().toString());
        assertThat(record.value()).contains(event.publicationId().toString()).contains("TELEGRAM");
        assertThat(header(record, "rabbot-event-id")).isEqualTo(eventId.toString());
        assertThat(header(record, "rabbot-event-type")).isEqualTo("PublishDue");
        assertThat(header(record, "rabbot-tenant-id")).isEqualTo(event.tenantId().toString());
    }

    @Test
    void eventIsSentOnlyOnce() {
        tx.executeWithoutResult(status -> outbox.publish(sampleEvent()));

        assertThat(relay.relayBatch()).isEqualTo(1);
        // Второй проход: отправленное уже не берётся.
        assertThat(relay.relayBatch()).isZero();
    }

    @Test
    void rolledBackEventIsNeverSent() {
        try {
            tx.executeWithoutResult(status -> {
                outbox.publish(sampleEvent());
                throw new IllegalStateException("откат");
            });
        } catch (IllegalStateException ignored) {
        }

        assertThat(relay.relayBatch()).isZero();
    }

    @Test
    void failedSendIsRecordedAndBlocksSameKeyInBatch() {
        // Два события с одним ключом: первое — в топик с недопустимым именем
        // (Kafka его отклонит), второе — нормальное.
        jdbc.update("""
                INSERT INTO core.outbox_event (aggregate_type, aggregate_id, event_type, topic, message_key, payload)
                VALUES ('Test', '1', 'Test', 'bad topic!', 'same-key', '{}'::jsonb)""");
        jdbc.update("""
                INSERT INTO core.outbox_event (aggregate_type, aggregate_id, event_type, topic, message_key, payload)
                VALUES ('Test', '2', 'Test', 'publish.due', 'same-key', '{}'::jsonb)""");

        assertThat(relay.relayBatch()).isZero();

        // Первое: попытка засчитана, причина записана.
        Map<String, Object> failed = jdbc.queryForMap(
                "SELECT attempts, last_error, published_at FROM core.outbox_event WHERE aggregate_id = '1'");
        assertThat(failed.get("attempts")).isEqualTo(1);
        assertThat((String) failed.get("last_error")).isNotBlank();
        assertThat(failed.get("published_at")).isNull();

        // Второе (тот же ключ) не отправлено, чтобы не обогнать первое.
        Map<String, Object> waiting = jdbc.queryForMap(
                "SELECT attempts, published_at FROM core.outbox_event WHERE aggregate_id = '2'");
        assertThat(waiting.get("attempts")).isEqualTo(0);
        assertThat(waiting.get("published_at")).isNull();
    }

    @Test
    void exhaustedEventIsNoLongerRetried() {
        UUID id = tx.execute(status -> outbox.publish(sampleEvent()));
        // max-attempts в тестах = 3.
        jdbc.update("UPDATE core.outbox_event SET attempts = 3 WHERE id = ?", id);

        assertThat(relay.relayBatch()).isZero();
    }
}