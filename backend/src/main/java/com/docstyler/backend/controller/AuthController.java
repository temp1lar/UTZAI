package com.docstyler.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

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

    @PostMapping("/process")
    public Map<String, String> processDocument(@RequestBody Map<String, String> request) {
        String template = request.get("template");
        String draft = request.get("draft");
        
        String result = "Обработанный документ (пока заглушка)";
        
        return Map.of(
            "status", "success",
            "message", "Нейросеть обработала документ",
            "result", result
        );
    }
}