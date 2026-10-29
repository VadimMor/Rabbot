package dev.rabbot.outbox;

import dev.rabbot.events.PublishDue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OutboxTestApplication.class)
@Import(OutboxTestApplication.Containers.class)
class OutboxPublisherIT {

    @Autowired
    OutboxPublisher outbox;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE core.outbox_event");
    }

    private static PublishDue sampleEvent() {
        return new PublishDue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "TELEGRAM", Instant.parse("2026-10-29T09:00:00Z"), 1);
    }

    @Test
    void writesEventRowWithTopicKeyPayloadAndHeaders() {
        PublishDue event = sampleEvent();

        UUID id = tx.execute(status -> outbox.publish(event));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT topic, message_key, event_type, aggregate_type, aggregate_id, tenant_id,
                       published_at, attempts,
                       payload ->> 'publicationId'  AS payload_publication,
                       payload ->> 'platform'       AS payload_platform,
                       payload ->> 'scheduledAt'    AS payload_scheduled,
                       jsonb_exists(payload, 'topic')       AS payload_has_topic,
                       headers ->> 'rabbot-event-id'        AS h_id,
                       headers ->> 'rabbot-event-type'      AS h_type,
                       headers ->> 'rabbot-schema-version'  AS h_version,
                       headers ->> 'rabbot-tenant-id'       AS h_tenant
                FROM core.outbox_event WHERE id = ?""", id);

        // Куда и с каким ключом relay отправит сообщение.
        assertThat(row.get("topic")).isEqualTo("publish.due");
        assertThat(row.get("message_key")).isEqualTo(event.channelId().toString());
        assertThat(row.get("event_type")).isEqualTo("PublishDue");
        assertThat(row.get("aggregate_type")).isEqualTo("Publication");
        assertThat(row.get("aggregate_id")).isEqualTo(event.publicationId().toString());
        assertThat(row.get("tenant_id")).isEqualTo(event.tenantId());

        // Ещё не отправлено.
        assertThat(row.get("published_at")).isNull();
        assertThat(row.get("attempts")).isEqualTo(0);

        // Тело — поля события, даты в ISO-8601, без служебных topic/key.
        assertThat(row.get("payload_publication")).isEqualTo(event.publicationId().toString());
        assertThat(row.get("payload_platform")).isEqualTo("TELEGRAM");
        assertThat(row.get("payload_scheduled")).isEqualTo("2026-10-29T09:00:00Z");
        assertThat(row.get("payload_has_topic")).isEqualTo(false);

        // Заголовки: id совпадает с id строки — по нему потребитель отсеет дубли.
        assertThat(row.get("h_id")).isEqualTo(id.toString());
        assertThat(row.get("h_type")).isEqualTo("PublishDue");
        assertThat(row.get("h_version")).isEqualTo("1");
        assertThat(row.get("h_tenant")).isEqualTo(event.tenantId().toString());
    }

    @Test
    void rolledBackTransactionLeavesNoEvent() {
        // Главная гарантия outbox: данные откатились — события тоже нет.
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            outbox.publish(sampleEvent());
            throw new IllegalStateException("бизнес-ошибка после записи события");
        })).isInstanceOf(IllegalStateException.class);

        Integer count = jdbc.queryForObject("SELECT count(*) FROM core.outbox_event", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void publishOutsideTransactionIsRejected() {
        // Без транзакции событие оторвалось бы от данных — запрещаем явно.
        assertThatThrownBy(() -> outbox.publish(sampleEvent()))
                .isInstanceOf(IllegalTransactionStateException.class);

        Integer count = jdbc.queryForObject("SELECT count(*) FROM core.outbox_event", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void eventsWithinOneTransactionAreAllStored() {
        tx.executeWithoutResult(status -> {
            outbox.publish(sampleEvent());
            outbox.publish(sampleEvent());
            outbox.publish(sampleEvent());
        });

        Integer count = jdbc.queryForObject("SELECT count(*) FROM core.outbox_event", Integer.class);
        assertThat(count).isEqualTo(3);
    }
}