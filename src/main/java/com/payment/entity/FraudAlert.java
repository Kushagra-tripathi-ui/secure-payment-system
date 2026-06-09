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
 * FRAUD ALERT ENTITY - Tracks suspicious activities
 * 
 * CONCEPT: Fraud Detection System
 * 
 * Basic fraud detection rules:
 * 1. Velocity check: Too many transactions in short time
 * 2. Amount check: Transaction amount unusually high
 * 3. Geographic check: Access from unusual location
 * 4. Pattern check: Unusual transaction pattern
 * 
 * This entity stores detected anomalies for investigation
 */
@Entity
@Table(name = "fraud_alerts", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    /**
     * ALERT TYPE: What kind of fraud risk?
     * VELOCITY: Too many transactions
     * AMOUNT: Amount too high
     * GEOGRAPHIC: Unusual location
     * PATTERN: Unusual pattern
     */
    @Column(nullable = false, length = 50)
    private String alertType;

    /**
     * RISK LEVEL: How serious?
     * LOW: Monitor
     * MEDIUM: Review
     * HIGH: Block and investigate
     */
    @Column(nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";  // OPEN, INVESTIGATING, RESOLVED, FALSE_ALARM

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String investigationNotes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
