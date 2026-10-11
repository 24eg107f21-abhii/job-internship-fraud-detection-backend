package com.abhinay.frauddetection.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhinay.frauddetection.dto.LoginRequest;
import com.abhinay.frauddetection.dto.RegisterRequest;
import com.abhinay.frauddetection.service.authservice;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class authcontroller {

    private final authservice authService;

    public authcontroller(authservice authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "User registered successfully",
                        "user", authService.register(request)
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        boolean success = authService.login(request);

        if (success) {
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password"));
    }
}