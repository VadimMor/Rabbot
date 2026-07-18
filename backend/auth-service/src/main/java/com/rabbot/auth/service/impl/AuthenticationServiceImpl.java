package com.rabbot.auth.service.impl;

import com.rabbot.auth.dto.request.LoginRequest;
import com.rabbot.auth.dto.request.RefreshTokenRequest;
import com.rabbot.auth.dto.request.RegisterRequest;
import com.rabbot.auth.dto.response.AuthResponse;

import com.rabbot.auth.Enum.RoleUser;
import com.rabbot.auth.model.User;

import com.rabbot.auth.repository.UserRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import com.rabbot.auth.service.AuthenticationService;
import com.rabbot.auth.service.JwtService;
import com.rabbot.auth.service.LoginHistory;
import com.rabbot.auth.Enum.LoginStatus;

@Service
@RequiredArgsConstructor
@Data
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtServiceImpl jwtServiceImpl;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginHistory loginHistory;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        var user = new User();
        user.setEmail(request.getEmail());
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
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        var user = userRepository.findByEmail(request.getEmail());

        if (user.isEmpty()) {
            loginHistory.saveLoginAttempt(request.getEmail(), null, httpRequest, LoginStatus.FAILED_USER_NOT_FOUND);
            throw new IllegalArgumentException("Неверный email или пароль");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {
            loginHistory.saveLoginAttempt(request.getEmail(), null, httpRequest, LoginStatus.FAILED_BAD_CREDENTIALS);
            throw new IllegalArgumentException("Неверный email или пароль");
        }

        loginHistory.saveLoginAttempt(request.getEmail(), user.get(), httpRequest, LoginStatus.SUCCESS);

        var accessToken = jwtService.generateToken(user.get());
        var refreshToken = jwtService.generateRefreshToken(user.get());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        String email = jwtService.extractEmail(refreshToken);

        if (email != null) {
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                var newAccessToken = jwtService.generateToken(user);
                var newRefreshToken = jwtService.generateRefreshToken(user);

                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }
        }

        throw new IllegalArgumentException("Невалидный или просроченный Refresh-токен");
    }
}