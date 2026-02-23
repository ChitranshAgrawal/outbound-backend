package com.addverb.outbound_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins.
     * Dev:  http://localhost:4200
     * Prod: https://your-domain.com
     *
     * Set via application.properties:
     *   app.cors.allowed-origins=https://your-domain.com
     */
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Exact origins only — never use allowedOriginPatterns("*") in production
        config.setAllowedOrigins(allowedOrigins);

        // All standard HTTP methods including OPTIONS (preflight)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers the frontend sends
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "Bearer"
        ));

        // Headers the frontend is allowed to read from the response
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        // Allow cookies / Authorization header to be sent cross-origin
        config.setAllowCredentials(true);

        // How long browsers cache preflight response (1 hour)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to every endpoint
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}




