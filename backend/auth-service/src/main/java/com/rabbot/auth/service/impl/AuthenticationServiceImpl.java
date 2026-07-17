package com.rabbot.auth.service.impl;

import com.rabbot.auth.dto.request.AuthResponse;
import com.rabbot.auth.dto.request.LoginRequest;
import com.rabbot.auth.dto.request.RegisterRequest;
import com.rabbot.auth.Enum.RoleUser;
import com.rabbot.auth.model.User;
import com.rabbot.auth.repository.UserRepository;
import com.rabbot.auth.service.AuthenticationService;
import com.rabbot.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        var user = new User();
        user.setEmail(request.getEmail());
        // Хэшируем пароль перед сохранением в БД
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleUser.ROLE_USER);

        var savedUser = userRepository.save(user);

        var accessToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));

        // Проверяем, совпадает ли сырой пароль из запроса с хэшем из базы
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Неверный email или пароль");
        }

        // TODO: В будущем добавим сюда сохранение в LoginHistoryRepository

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}