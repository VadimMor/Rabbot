package com.rabbot.auth.service.impl;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import com.rabbot.auth.Enum.LoginStatus;
import com.rabbot.auth.repository.LoginHistoryRepository;
import com.rabbot.auth.service.LoginHistory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ua_parser.Client;
import ua_parser.Parser;

import com.rabbot.auth.model.LoginHistoryModel;
import com.rabbot.auth.model.User;


@Service
@RequiredArgsConstructor
@Data
public class LoginHistoryImpl implements LoginHistory {
    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public void saveLoginAttempt(String email, User user, HttpServletRequest request, LoginStatus status) {
        LoginHistoryModel history = new LoginHistoryModel();
        history.setEmailAttempt(email);
        history.setUser(user);
        history.setStatus(status);

        // 1. Получаем IP-адрес (учитываем, что запрос мог пройти через Nginx/Gateway)
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Если IP содержит несколько адресов (цепочка прокси), берем первый
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        history.setIpAddress(ipAddress);

        // 2. Получаем сырой User-Agent
        String userAgentString = request.getHeader("User-Agent");
        history.setUserAgent(userAgentString);

        // 3. Парсим User-Agent с помощью библиотеки
        if (userAgentString != null && !userAgentString.isEmpty()) {
            try {
                Parser uaParser = new Parser();
                Client client = uaParser.parse(userAgentString);

                String browserFamily = client.userAgent.family != null ? client.userAgent.family : "Unknown";
                String browserMajor = client.userAgent.major != null ? " " + client.userAgent.major : "";
                history.setBrowser((browserFamily + browserMajor).trim());

                String osFamily = client.os.family != null ? client.os.family : "Unknown";
                String osMajor = client.os.major != null ? " " + client.os.major : "";
                history.setOperatingSystem((osFamily + osMajor).trim());

                history.setDeviceType(client.device.family != null ? client.device.family : "Unknown");
            } catch (Exception e) {
                // Если парсер упал, оставляем поля пустыми, но сырой User-Agent уже сохранен
                System.err.println("Ошибка парсинга User-Agent: " + e.getMessage());
            }
        }

        // 4. Сохраняем в БД
        loginHistoryRepository.save(history);
    }
}
