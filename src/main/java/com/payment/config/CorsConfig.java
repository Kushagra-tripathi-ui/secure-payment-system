package com.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS CONFIG - allows the standalone frontend (index.html, opened directly
 * in a browser or served from a different origin like file:// or :5500)
 * to call this API running on :8080.
 *
 * CONCEPT: Browsers block cross-origin requests by default (CORS policy).
 * Since our frontend is a static HTML file with no build step, it doesn't
 * share an origin with the API — so we explicitly allow it here.
 *
 * In production you'd restrict allowedOrigins to your real frontend domain.
 * For a demo/interview setup, "*" keeps things simple.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
