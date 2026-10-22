package dev.rabbot.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Точка входа. @SpringBootApplication включает автоконфигурацию
// и сканирует все классы в пакете dev.rabbot.api и ниже.
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}