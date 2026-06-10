package com.payment.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * @CurrentUser ANNOTATION - Custom annotation to inject logged-in user's ID
 *
 * CONCEPT: Custom Argument Resolver
 *
 * Without this:
 *   You'd have to manually call SecurityContextHolder.getContext().getAuthentication()
 *   in every controller method — repetitive and messy.
 *
 * With this:
 *   Just add @CurrentUser Long userId to any controller method parameter.
 *   Spring automatically reads the authenticated user's ID from the JWT token
 *   (which was set in SecurityContext by JwtAuthenticationFilter).
 *
 * @AuthenticationPrincipal: tells Spring to resolve this from SecurityContext.
 *   The "principal" is whatever we stored during authentication — in our case,
 *   it's the userId (Long) we stored in UsernamePasswordAuthenticationToken.
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
