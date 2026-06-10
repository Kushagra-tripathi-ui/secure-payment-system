package com.payment.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SECURITY CONFIG - Central Spring Security configuration
 *
 * CONCEPT: Why do we need this?
 *
 * By default, Spring Security blocks ALL requests and shows a login form.
 * We need to tell it:
 * 1. Which endpoints are public (register, login)
 * 2. Which endpoints require authentication (payments)
 * 3. Don't use sessions (we use JWT — stateless)
 * 4. Run our JwtAuthenticationFilter before every request
 *
 * @EnableWebSecurity:    Activates Spring Security
 * @EnableMethodSecurity: Allows @PreAuthorize annotations on controller methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * PUBLIC ENDPOINTS - no token needed
     * These are open to everyone — registration and login.
     * Note: context-path is /api (set in application.yml),
     *       so /auth/register maps to real path /api/auth/register
     */
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/register",
        "/api/auth/login",
        "/v3/api-docs/**",    // Swagger API docs
        "/swagger-ui/**",     // Swagger UI
        "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // DISABLE CSRF: Not needed for stateless REST APIs
            // CSRF protects browser-based session attacks.
            // Since we use JWT (no cookies/sessions), CSRF is irrelevant.
            .csrf(AbstractHttpConfigurer::disable)

            // STATELESS SESSION: Don't create/use HTTP sessions
            // Every request must carry its own JWT token.
            // This makes the app horizontally scalable (any server can handle any request).
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // AUTHORIZATION RULES
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()  // Public
                .anyRequest().authenticated()                    // Everything else needs JWT
            )

            // ADD JWT FILTER
            // Run JwtAuthenticationFilter BEFORE Spring's default login filter.
            // Our filter extracts userId from JWT and sets it in SecurityContext,
            // so by the time the controller runs, @CurrentUser already works.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
