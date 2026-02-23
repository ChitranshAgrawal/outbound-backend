package com.addverb.outbound_service.auth.dto;

import com.addverb.outbound_service.auth.enums.UserRole;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresInSeconds,
        UserProfileResponse user
) {
    public static AuthResponse of(String accessToken, Long expiresInSeconds, UserProfileResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }

    public record UserProfileResponse(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            UserRole role
    ) {}
}
