package dev.rabbot.events;

import java.util.UUID;

// Общий контракт события между сервисами.
// Каждое событие само знает, в какой топик идёт и с каким ключом.
public interface IntegrationEvent {

    // Топик Kafka (константа из Topics).
    String topic();

    // Ключ сообщения: события с одним ключом попадают в одну партицию и читаются строго по порядку.
    String key();

    // Тип события для заголовка rabbot-event-type.
    String eventType();

    // Версия схемы тела.
    int schemaVersion();

    // Организация (для заголовка и outbox_event.tenant_id).
    UUID tenantId();

    // Сущность, о которой событие: тип и id (outbox_event.aggregate_*).
    String aggregateType();

    String aggregateId();
}