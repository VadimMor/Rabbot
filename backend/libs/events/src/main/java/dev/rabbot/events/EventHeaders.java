package dev.rabbot.events;

// Имена заголовков Kafka-сообщения. В заголовках — служебные данные,
// в теле — только сами данные события.
public final class EventHeaders {

    // UUID события. По нему потребитель отсекает дубли (таблица processed_message).
    public static final String EVENT_ID = "rabbot-event-id";

    // Тип события, например "PublishDue".
    public static final String EVENT_TYPE = "rabbot-event-type";

    // Версия схемы тела. Меняем поля несовместимо — повышаем версию.
    public static final String SCHEMA_VERSION = "rabbot-schema-version";

    // Организация. У системных событий может отсутствовать.
    public static final String TENANT_ID = "rabbot-tenant-id";

    // Когда событие произошло (ISO-8601), а не когда отправлено.
    public static final String OCCURRED_AT = "rabbot-occurred-at";

    private EventHeaders() {
    }
}