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
 * ACCOUNT ENTITY - Represents a wallet/account for a user
 * 
 * CONCEPT: One user can have multiple accounts (checking, savings, etc)
 * This is where money is stored and transactions are performed
 * 
 * Database Design:
 * - accountNumber: Unique identifier (like bank account number)
 * - balance: Current money in account (DECIMAL for precision)
 * - version: For optimistic locking (prevents concurrent update issues)
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FOREIGN KEY: Links account to user
     * @ManyToOne: Many accounts belong to one user
     * fetch = FetchType.LAZY: Don't load user data unless explicitly requested (performance)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false)
    private String accountType;  // "savings", "checking", etc

    /**
     * OPTIMISTIC LOCKING CONCEPT:
     * 
     * Problem: Two concurrent requests both read balance = 1000
     *          Both subtract 500, both update to 500 (lost 500!)
     * 
     * Solution: Use @Version column
     * - When updating, Hibernate adds: WHERE version = <current_version>
     * - After update, version increments
     * - If version changed, update fails (someone else modified it)
     * - Application retries transaction
     * 
     * This prevents lost updates in high-concurrency scenarios
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    /**
     * PRECISION REQUIREMENT:
     * Use BigDecimal for money, NOT double/float
     * - double: 0.1 + 0.2 = 0.30000000000000004 (precision loss!)
     * - BigDecimal: exact decimal representation
     * - columnDefinition: tells MySQL to use DECIMAL(19,2) = $X,XXX,XXX.XX
     */
    @Column(nullable = false, columnDefinition = "DECIMAL(19,2)")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String notes;
}
