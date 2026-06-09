package com.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AUDIT LOG ENTITY - Tracks all important system actions
 * 
 * CONCEPT: Audit Trail for compliance and security
 * 
 * Why? 
 * - Regulatory compliance (PCI-DSS for payments)
 * - Security: detect unauthorized access
 * - Debugging: understand what happened and when
 * - Fraud investigation: trace money flow
 * 
 * What to log:
 * - User login/logout
 * - Account creation/modification
 * - Transaction start/completion/failure
 * - Balance changes
 * - Failed authentication attempts
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * ACTION: What happened
     * Examples: USER_LOGIN, TRANSACTION_INITIATED, ACCOUNT_CREATED, etc
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * ENTITY TYPE: What was affected
     * Examples: USER, ACCOUNT, TRANSACTION, etc
     */
    @Column(nullable = false, length = 30)
    private String entityType;

    /**
     * ENTITY ID: Which specific record
     */
    @Column(nullable = false)
    private Long entityId;

    /**
     * OLD VALUE: State before action (for debugging changes)
     */
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    /**
     * NEW VALUE: State after action
     */
    @Column(columnDefinition = "TEXT")
    private String newValue;

    /**
     * DETAILS: Additional context
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * IP ADDRESS: Where request came from
     * For security: detect if account accessed from unusual location
     */
    @Column(length = 50)
    private String ipAddress;

    /**
     * USER AGENT: Browser/client info
     */
    @Column(length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
