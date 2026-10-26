package dev.rabbot.api.enums;

// Статус подключения канала. Совпадает с CHECK в таблице channel (V4).
public enum ChannelStatus {
    CONNECTED,      // всё работает
    NEEDS_REAUTH,   // токен истёк, нужно переподключить
    ERROR,          // последняя публикация упала
    DISCONNECTED    // отключён пользователем
}