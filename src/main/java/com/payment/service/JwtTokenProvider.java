package com.payment.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret:your-secret-key-must-be-at-least-32-characters-long-for-hs256}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}") // 24 hours in ms
    private int jwtExpirationMs;
    
    /**
     * Generate JWT token
     */
    public String generateToken(Long userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        String token = Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("email", email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
        
        log.debug("Generated token for user: {}", userId);
        return token;
    }
    
    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
