package com.payment.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT TOKEN PROVIDER - Handles JWT token generation and validation
 *
 * JWT (JSON Web Token) is a stateless authentication mechanism
 *
 * Structure: header.payload.signature
 * - header: Token type and hashing algorithm
 * - payload: Claims (user info, expiration, etc)
 * - signature: Cryptographic signature to prevent tampering
 *
 * Advantages:
 * - Stateless: No session storage needed
 * - Scalable: Works across multiple servers
 * - Mobile-friendly: Easy to use with mobile apps
 *
 * NOTE: Uses JJWT 0.12.x API (Jwts.parser()/verifyWith(), builder().subject()).
 * The older 0.11.x API (parserBuilder(), setSubject()) was removed in 0.12.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:your-secret-key-must-be-at-least-32-characters-long-for-hs256}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 24 hours in ms
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate JWT token
     */
    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();

        log.debug("Generated token for user: {}", userId);
        return token;
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
