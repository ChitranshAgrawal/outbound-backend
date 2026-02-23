package com.addverb.outbound_service.auth.service;

import com.addverb.outbound_service.auth.dto.AuthResponse;
import com.addverb.outbound_service.auth.dto.LoginRequest;
import com.addverb.outbound_service.auth.dto.SignupRequest;
import com.addverb.outbound_service.auth.entity.RevokedToken;
import com.addverb.outbound_service.auth.entity.User;
import com.addverb.outbound_service.auth.enums.UserRole;
import com.addverb.outbound_service.auth.exception.AuthException;
import com.addverb.outbound_service.auth.repository.RevokedTokenRepository;
import com.addverb.outbound_service.auth.repository.UserRepository;
import com.addverb.outbound_service.auth.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new AuthException("Username is already taken", HttpStatus.CONFLICT);
        }

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new AuthException("Email is already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase(Locale.ROOT))
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .role(UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.of(token, jwtService.getAccessTokenExpirationSeconds(), toProfile(savedUser));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().trim();
        User user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new AuthException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid username/email or password");
        }

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new AuthException("Your account is disabled or locked", HttpStatus.FORBIDDEN);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getAccessTokenExpirationSeconds(), toProfile(user));
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
        try {
            String tokenHash = jwtService.sha256(token);
            if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {
                revokedTokenRepository.save(RevokedToken.builder()
                        .tokenHash(tokenHash)
                        .expiresAt(LocalDateTime.ofInstant(jwtService.extractExpiration(token), ZoneOffset.UTC))
                        .revokedAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build());
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Ignore malformed token during logout to keep endpoint idempotent.
        }
    }

    @Override
    public AuthResponse.UserProfileResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AuthException("User not found"));

        return toProfile(user);
    }

    private AuthResponse.UserProfileResponse toProfile(User user) {
        return new AuthResponse.UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole()
        );
    }
}




