package com.rabbot.auth.service;

import com.rabbot.auth.model.User;
import java.util.Map;

public interface JwtService {
    
    String extractEmail(String token);

    String generateToken(User user);

    String generateToken(Map<String, Object> extraClaims, User user);

    String generateRefreshToken(User user);

    boolean isTokenValid(String token, User user);
}
