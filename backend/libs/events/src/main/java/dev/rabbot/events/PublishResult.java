package dev.rabbot.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Результат попытки публикации. Отправляет publisher, читают scheduler
public record PublishResult(
        UUID publicationId,
        UUID tenantId,
        UUID channelId,
        int attempt,
        PublishOutcome outcome,
        // id поста в соцсети — только при PUBLISHED.
        String externalPostId,
        // Текст ошибки — только при RETRY и FAILED.
        String error,
        Instant finishedAt
) implements IntegrationEvent {

    public static final String TYPE = "PublishResult";
    public static final int VERSION = 1;

    public PublishResult {
        Objects.requireNonNull(publicationId, "publicationId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(finishedAt, "finishedAt");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt должен быть >= 1, а не " + attempt);
        }
        // Согласованность полей с итогом.
        if (outcome == PublishOutcome.PUBLISHED && (externalPostId == null || externalPostId.isBlank())) {
            throw new IllegalArgumentException("PUBLISHED без externalPostId");
        }
        if (outcome != PublishOutcome.PUBLISHED && (error == null || error.isBlank())) {
            throw new IllegalArgumentException(outcome + " без текста ошибки");
        }
    }

    public static PublishResult published(PublishDue due, String externalPostId, Instant at) {
        return new PublishResult(due.publicationId(), due.tenantId(), due.channelId(),
                due.attempt(), PublishOutcome.PUBLISHED, externalPostId, null, at);
    }

    public static PublishResult retry(PublishDue due, String error, Instant at) {
        return new PublishResult(due.publicationId(), due.tenantId(), due.channelId(),
                due.attempt(), PublishOutcome.RETRY, null, error, at);
    }

    public static PublishResult failed(PublishDue due, String error, Instant at) {
        return new PublishResult(due.publicationId(), due.tenantId(), due.channelId(),
                due.attempt(), PublishOutcome.FAILED, null, error, at);
    }

    @Override public String topic() { return Topics.PUBLISH_RESULT; }
    @Override public String key() { return publicationId.toString(); }
    @Override public String eventType() { return TYPE; }
    @Override public int schemaVersion() { return VERSION; }
    @Override public String aggregateType() { return "Publication"; }
    @Override public String aggregateId() { return publicationId.toString(); }
}