package com.docstyler.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/health")
    public String health() {
        return "✅ DocStyler Backend работает! 🚀";
    }

    @GetMapping("/test")
    public String test() {
        return "Тестовый endpoint работает!";
    }
}