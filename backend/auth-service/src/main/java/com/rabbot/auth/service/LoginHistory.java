package com.rabbot.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import com.rabbot.auth.model.User;
import com.rabbot.auth.Enum.LoginStatus;

public interface LoginHistory {
    void saveLoginAttempt(String email, User user, HttpServletRequest request, LoginStatus status);
}
