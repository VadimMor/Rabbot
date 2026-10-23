plugins {
    // Обычный Java-проект.
    java
    // Spring Boot: задачи bootRun, bootJar и управление версиями библиотек.
    id("org.springframework.boot") version "4.0.0"
}

group = "dev.rabbot"
version = "0.1.0-SNAPSHOT"

java {
    // Компилируем и запускаем на Java 25.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // BOM Spring Boot: версии всех starter-ов ниже берутся из него.
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    // REST-контроллеры (Spring MVC + встроенный Tomcat).
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // JPA + Hibernate для работы с PostgreSQL.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Валидация входных данных (@NotBlank, @Size и т. д.).
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // /actuator/health — проверка, что сервис жив (пригодится для Kubernetes).
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // JDBC-драйвер PostgreSQL нужен только во время работы.
    runtimeOnly("org.postgresql:postgresql")

    // ---------- тесты ----------
    // JUnit 5, AssertJ, Mockito, JsonPath.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // MockMvc: HTTP-запросы к контроллерам без настоящего сетевого порта.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // Связка Spring Boot + Testcontainers (@ServiceConnection).
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // Flyway только в тестах: накатить миграции на чистую базу в контейнере.
    // В основном коде его нет — там схему меняет отдельный контейнер flyway.
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Печатать в консоль каждый тест и полный текст ошибок.
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}