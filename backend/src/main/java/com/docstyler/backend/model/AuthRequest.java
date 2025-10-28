package com.docstyler.backend.model;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "Email обязателен")
    @Email(message = "Введите корректный email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}