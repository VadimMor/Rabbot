package com.rabbot.auth.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}