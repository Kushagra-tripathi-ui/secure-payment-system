package com.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * USER ENTITY - Represents a user in the payment system
 * 
 * @Entity: Tells Hibernate this is a database table
 * @Table: Specifies table name and indexes for performance
 * 
 * CONCEPT: One user can have multiple accounts/wallets
 * A user is the person who registers (with email, password)
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_phone", columnList = "phone")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;  // Will be stored as BCrypt hash

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * AUDIT COLUMNS: Track when records were created/modified
     * @CreationTimestamp: Hibernate automatically sets this on insert
     * @UpdateTimestamp: Hibernate automatically updates this on modify
     * These help track data changes for compliance and debugging
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String notes;
}
