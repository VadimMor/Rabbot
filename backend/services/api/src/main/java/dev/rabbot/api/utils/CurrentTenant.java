package dev.rabbot.api.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Метка для параметра контроллера: "подставь сюда id организации из запроса".
// Подставляет её CurrentTenantArgumentResolver (пакет config).
@Target(ElementType.PARAMETER)          // ставится только на параметры метода
@Retention(RetentionPolicy.RUNTIME)     // видна Spring во время работы
public @interface CurrentTenant {
}