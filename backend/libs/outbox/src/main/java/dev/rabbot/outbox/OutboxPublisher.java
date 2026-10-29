package dev.rabbot.outbox;

import dev.rabbot.events.IntegrationEvent;

import java.util.UUID;

// Записать событие в outbox. Отправку в Kafka делает relay отдельно.
public interface OutboxPublisher {

    // Возвращает id события — он же уйдёт в Kafka в заголовке rabbot-event-id
    UUID publish(IntegrationEvent event);
}