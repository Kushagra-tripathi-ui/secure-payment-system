package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application Entry Point
 * 
 * @SpringBootApplication: Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
 * This tells Spring to:
 * 1. Look for @Component classes in this package and subpackages
 * 2. Auto-configure Spring based on classpath (e.g., MySQL driver found = configure DataSource)
 * 3. Enable component scanning for dependency injection
 */
@SpringBootApplication
public class PaymentSystemApplication {

    public static void main(String[] args) {
        // SpringApplication.run() starts the embedded Tomcat server and initializes Spring context
        SpringApplication.run(PaymentSystemApplication.class, args);
    }

    /**
     * PASSWORD SECURITY CONCEPT:
     * 
     * Why do we need PasswordEncoder?
     * - NEVER store plain-text passwords in database
     * - BCrypt is a one-way hashing algorithm (cannot decrypt)
     * - Each time you hash, you get different output (includes random salt)
     * - Login: hash provided password and compare with stored hash
     * 
     * Spring Security uses this bean to:
     * 1. Encode password when user registers
     * 2. Compare encoded password during login
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
