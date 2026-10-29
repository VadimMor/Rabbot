package dev.rabbot.outbox;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;

// Автоконфигурация: сервис добавляет зависимость implementation(project(":outbox")),
// и OutboxPublisher сразу можно внедрять — без @Import и @ComponentScan.
// Spring Boot находит этот класс через файл
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
@AutoConfiguration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    // @ConditionalOnMissingBean: сервис может подменить реализацию своей.
    @Bean
    @ConditionalOnMissingBean
    OutboxPublisher outboxPublisher(DataSource dataSource,
                                    ObjectProvider<JsonMapper> jsonMapper,
                                    ObjectProvider<Clock> clock,
                                    OutboxProperties properties) {
        return new JdbcOutboxPublisher(
                // JdbcClient поверх того же DataSource: в транзакции Spring
                // он получит тот же коннект, что и JPA/JdbcTemplate сервиса.
                JdbcClient.create(dataSource),
                // JsonMapper Spring Boot (с настройками сервиса), а если его нет — свой.
                jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()),
                clock.getIfAvailable(Clock::systemUTC),
                properties);
    }

    // Relay создаётся, только если в сервисе rabbot.outbox.relay.enabled = true.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "rabbot.outbox.relay", name = "enabled", havingValue = "true")
    static class RelayConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OutboxRelay outboxRelay(DataSource dataSource,
                                KafkaTemplate<String, String> kafka,
                                PlatformTransactionManager transactionManager,
                                ObjectProvider<JsonMapper> jsonMapper,
                                OutboxProperties properties) {
            return new OutboxRelay(
                    JdbcClient.create(dataSource),
                    kafka,
                    new TransactionTemplate(transactionManager),
                    jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()),
                    properties);
        }

        @Bean
        @ConditionalOnMissingBean
        OutboxRelayRunner outboxRelayRunner(OutboxRelay relay, OutboxProperties properties) {
            return new OutboxRelayRunner(relay, properties);
        }
    }
}