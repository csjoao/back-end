package com.hyperativa.controller;

import com.hyperativa.dto.ApiResponse;
import com.hyperativa.dto.AuthRequest;
import com.hyperativa.dto.AuthResponse;
import com.hyperativa.service.contract.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login request for user: {}", authRequest.getUsername());
        AuthResponse response = userService.authenticate(authRequest);
        log.info("Login successful for user: {}", authRequest.getUsername());

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Register request for user: {}", authRequest.getUsername());
        userService.registerUser(authRequest.getUsername(), authRequest.getPassword());
        log.info("Registration successful for user: {}", authRequest.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "User registered successfully", HttpStatus.CREATED.value()));
    }
}
