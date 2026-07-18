package com.rabbot.auth.controller;

import com.rabbot.auth.dto.request.LoginRequest;
import com.rabbot.auth.dto.request.RefreshTokenRequest;
import com.rabbot.auth.dto.request.RegisterRequest;
import com.rabbot.auth.dto.response.AuthResponse;
import com.rabbot.auth.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
            authenticationService.login(
                request,
                httpRequest
            )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshToken) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }
}