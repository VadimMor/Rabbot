package dev.rabbot.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Конфигурация только для тестов: описывает контейнер с PostgreSQL.
// proxyBeanMethods = false — Spring не оборачивает класс прокси (быстрее старт).
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Контейнер — обычный Spring-бин. Spring сам запустит его до старта
    // приложения и остановит в конце.
    // @ServiceConnection: Spring Boot возьмёт из контейнера URL, логин и пароль
    // и подставит их в datasource вместо значений из application.yml.
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        // Та же версия, что в docker compose, чтобы тесты проверяли то же, что в бою.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }
}