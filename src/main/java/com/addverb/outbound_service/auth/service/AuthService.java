package com.addverb.outbound_service.auth.service;

import com.addverb.outbound_service.auth.dto.AuthResponse;
import com.addverb.outbound_service.auth.dto.LoginRequest;
import com.addverb.outbound_service.auth.dto.SignupRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    void logout(HttpServletRequest request);
    AuthResponse.UserProfileResponse me();
}

