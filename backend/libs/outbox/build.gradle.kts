plugins {
    `java-library`
}

group = "dev.rabbot"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Версии Spring, Jackson, Testcontainers — из BOM Spring Boot, как в api.
    val bootBom = platform("org.springframework.boot:spring-boot-dependencies:4.0.0")
    implementation(bootBom)
    testImplementation(bootBom)

    // в publish() передаётся IntegrationEvent.
    api(project(":events"))

    // Автоконфигурация и @ConfigurationProperties.
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // JdbcClient и транзакции.
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")
    // JSON. Jackson 3 (пакет tools.jackson), как во всём Spring Boot 4.
    implementation("tools.jackson.core:jackson-databind")

    // Relay отправляет события через KafkaTemplate.
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.slf4j:slf4j-api")


    // ---------- тесты: настоящий PostgreSQL с нашими миграциями ----------
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-kafka")
    testImplementation("org.testcontainers:testcontainers-kafka")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}