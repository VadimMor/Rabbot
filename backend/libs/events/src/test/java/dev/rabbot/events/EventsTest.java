package dev.rabbot.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventsTest {

    private static final Instant NOW = Instant.parse("2026-10-28T09:00:00Z");

    private static PublishDue due(int attempt) {
        return new PublishDue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "TELEGRAM", NOW, attempt);
    }

    @Test
    void publishDueGoesToItsTopicKeyedByChannel() {
        PublishDue due = due(1);

        assertThat(due.topic()).isEqualTo("publish.due");
        // Один канал — одна партиция — строгий порядок постов.
        assertThat(due.key()).isEqualTo(due.channelId().toString());
        assertThat(due.aggregateId()).isEqualTo(due.publicationId().toString());
        assertThat(due.eventType()).isEqualTo("PublishDue");
    }

    @Test
    void publishDueRejectsBrokenData() {
        assertThatThrownBy(() -> due(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublishDue(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "VK", NOW, 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resultFactoriesKeepIdsAndAttempt() {
        PublishDue due = due(3);

        PublishResult ok = PublishResult.published(due, "4021", NOW);
        assertThat(ok.publicationId()).isEqualTo(due.publicationId());
        assertThat(ok.attempt()).isEqualTo(3);
        assertThat(ok.outcome()).isEqualTo(PublishOutcome.PUBLISHED);
        assertThat(ok.topic()).isEqualTo("publish.result");
        assertThat(ok.key()).isEqualTo(due.publicationId().toString());

        PublishResult retry = PublishResult.retry(due, "429 Too Many Requests", NOW);
        assertThat(retry.outcome()).isEqualTo(PublishOutcome.RETRY);
        assertThat(retry.externalPostId()).isNull();
    }

    @Test
    void resultFieldsMustMatchOutcome() {
        PublishDue due = due(1);

        // Опубликован, но без id поста — так не бывает.
        assertThatThrownBy(() -> PublishResult.published(due, " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        // Ошибка без текста — непонятно, что чинить.
        assertThatThrownBy(() -> PublishResult.failed(due, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topicNamesMatchKafkaSetupScript() {
        assertThat(Topics.PUBLISH_DUE).isEqualTo("publish.due");
        assertThat(Topics.PUBLISH_RESULT).isEqualTo("publish.result");
        assertThat(Topics.PUBLISH_DUE_DLQ).isEqualTo("publish.due.dlq");
        assertThat(Topics.OUTBOX_RELAY_DLQ).isEqualTo("outbox.relay.dlq");
    }
}