package com.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TRANSACTION ENTITY - Represents a payment transaction
 * 
 * KEY CONCEPT: IDEMPOTENCY KEY for preventing duplicate transactions
 * 
 * Problem: User clicks "Send" button twice due to slow network
 *          System processes payment twice (user loses double amount)
 * 
 * Solution: idempotencyKey
 * - Client generates unique ID (UUID) for each transaction request
 * - Server checks if idempotencyKey already exists
 * - If exists, return previous response (don't process again)
 * - If new, process and store with this key
 * 
 * This makes the API "idempotent": calling it multiple times with same
 * idempotencyKey produces same result as calling once
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_from_account", columnList = "fromAccount_id"),
    @Index(name = "idx_to_account", columnList = "toAccount_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * IDEMPOTENCY KEY - Client-provided unique identifier
     * Prevents duplicate transaction processing
     */
    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fromAccount_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toAccount_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, columnDefinition = "DECIMAL(19,2)")
    private BigDecimal amount;

    /**
     * TRANSACTION STATUS - Tracks payment lifecycle
     * INITIATED -> PROCESSING -> COMPLETED/FAILED
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "INITIATED";  // INITIATED, PROCESSING, COMPLETED, FAILED

    @Column(length = 500)
    private String description;

    /**
     * REFERENCE FOR TRACKING
     * Used to trace transaction in external systems or logs
     */
    @Column(nullable = false, unique = true, length = 50)
    private String referenceNumber;

    /**
     * FAILURE REASON
     * If transaction fails, this explains why
     */
    @Column(length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime completedAt;
}
