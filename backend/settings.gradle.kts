// Плагин сам скачает JDK 25, если его нет на машине (через toolchain).
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Имя всего проекта.
rootProject.name = "rabbot"

// Подключаем сервис api. Имя модуля ":api", а лежит он в services/api.
// Каждый новый сервис добавляется сюда двумя такими строками.
include(":api")
project(":api").projectDir = file("services/api")