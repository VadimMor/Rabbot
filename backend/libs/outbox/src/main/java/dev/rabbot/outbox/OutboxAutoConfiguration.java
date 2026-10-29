package dev.rabbot.outbox;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

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
}