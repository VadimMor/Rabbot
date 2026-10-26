package dev.rabbot.api.enums;

// Площадки соцсетей. Значения совпадают с DOMAIN social_platform в БД (V1),
// поэтому храним как строку: @Enumerated(EnumType.STRING).
public enum SocialPlatform {
    TELEGRAM,
    VK,
    INSTAGRAM,
    TIKTOK,
    LINKEDIN,
    YOUTUBE,
    DZEN,
    OK
}