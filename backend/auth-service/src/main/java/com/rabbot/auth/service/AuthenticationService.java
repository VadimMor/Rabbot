package com.rabbot.auth.service;

import com.rabbot.auth.dto.request.LoginRequest;
import com.rabbot.auth.dto.request.RefreshTokenRequest;
import com.rabbot.auth.dto.request.RegisterRequest;
import com.rabbot.auth.dto.response.AuthResponse;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest refreshToken);
}