package dev.rabbot.api.config;

import dev.rabbot.api.utils.CurrentTenant;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

// Достаёт id организации из заголовка X-Tenant-Id и подставляет его
// в параметры с @CurrentTenant.
// Временное решение: после подключения Keycloak tenant будет браться из JWT-токена,
// а контроллеры не изменятся — поменяется только этот класс.
public class CurrentTenantArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Tenant-Id";

    // Срабатываем только для параметров с @CurrentTenant типа UUID.
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentTenant.class)
                && parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String value = webRequest.getHeader(HEADER);

        // Нет заголовка → 400 Bad Request.
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нет заголовка " + HEADER);
        }

        // Заголовок не UUID → тоже 400.
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HEADER + " должен быть UUID");
        }
    }
}