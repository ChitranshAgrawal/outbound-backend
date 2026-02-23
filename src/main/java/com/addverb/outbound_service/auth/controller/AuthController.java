package com.addverb.outbound_service.auth.controller;

import com.addverb.outbound_service.auth.dto.AuthResponse;
import com.addverb.outbound_service.auth.dto.LoginRequest;
import com.addverb.outbound_service.auth.dto.SignupRequest;
import com.addverb.outbound_service.auth.service.AuthService;
import com.addverb.outbound_service.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Signup successful")
                .data(response)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Logout successful")
                .data(null)
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserProfileResponse>> me() {
        AuthResponse.UserProfileResponse response = authService.me();
        return ResponseEntity.ok(ApiResponse.<AuthResponse.UserProfileResponse>builder()
                .success(true)
                .message("User profile fetched")
                .data(response)
                .build());
    }
}



