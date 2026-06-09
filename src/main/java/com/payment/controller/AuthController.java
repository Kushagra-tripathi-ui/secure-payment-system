package com.payment.controller;

import com.payment.dto.RegisterRequest;
import com.payment.dto.LoginRequest;
import com.payment.dto.AuthResponse;
import com.payment.dto.ApiResponse;
import com.payment.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * AUTH CONTROLLER - REST endpoints for registration and login
 * 
 * API Endpoints:
 * POST /api/auth/register - Register new user
 * POST /api/auth/login - Login user
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
