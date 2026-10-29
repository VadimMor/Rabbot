package dev.rabbot.events;

// Имена топиков Kafka. Должны совпадать с deploy/compose/kafka/create-topics.sh.
// Строки только здесь - чтобы опечатка в имени топика была невозможна.
public final class Topics {

    // Тренды
    public static final String TREND_RAW = "trend.raw";
    public static final String TREND_ENRICHED = "trend.enriched";

    // Генерация контента
    public static final String CONTENT_REQUESTED = "content.requested";
    public static final String CONTENT_GENERATED = "content.generated";

    // Публикация
    public static final String PUBLISH_DUE = "publish.due";
    public static final String PUBLISH_RESULT = "publish.result";
    public static final String PUBLISH_DUE_DLQ = "publish.due.dlq";

    // Аналитика
    public static final String METRICS_RAW = "metrics.raw";

    // Сообщения, которые relay не смог отправить из outbox.
    public static final String OUTBOX_RELAY_DLQ = "outbox.relay.dlq";

    private Topics() {
    }
}