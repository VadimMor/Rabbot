package com.rabbot.auth.service;

import com.rabbot.auth.dto.request.AuthResponse;
import com.rabbot.auth.dto.request.LoginRequest;
import com.rabbot.auth.dto.request.RegisterRequest;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}