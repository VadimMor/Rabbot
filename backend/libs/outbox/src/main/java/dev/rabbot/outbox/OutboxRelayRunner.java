package dev.rabbot.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Запускает relay по расписанию, пока работает приложение.
public class OutboxRelayRunner implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayRunner.class);

    private final OutboxRelay relay;
    private final OutboxProperties.Relay config;
    private ScheduledExecutorService executor;
    private volatile boolean running;

    public OutboxRelayRunner(OutboxRelay relay, OutboxProperties properties) {
        this.relay = relay;
        this.config = properties.relay();
    }

    @Override
    public void start() {
        // Один виртуальный поток: проходы не пересекаются.
        executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("outbox-relay").factory());
        long interval = config.pollInterval().toMillis();
        // Первый проход — через interval, затем пауза interval после окончания предыдущего.
        executor.scheduleWithFixedDelay(this::drain, interval, interval, TimeUnit.MILLISECONDS);
        running = true;
        log.info("Outbox relay запущен: каждые {} мс, пачка {}", interval, config.batchSize());
    }

    // Отправлять пачки, пока они полные: после простоя очередь разгребается
    // сразу, а не по одной пачке в полсекунды.
    private void drain() {
        try {
            int sent;
            do {
                sent = relay.relayBatch();
            } while (sent == config.batchSize());
        } catch (Exception e) {
            // Исключение не должно убить расписание: следующий проход попробует снова.
            log.error("Outbox relay: проход завершился ошибкой", e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}