package com.payment.service;

import com.payment.dto.TransactionRequest;
import com.payment.dto.TransactionResponse;
import com.payment.entity.Account;
import com.payment.entity.Transaction;
import com.payment.entity.FraudAlert;
import com.payment.entity.AuditLog;
import com.payment.repository.AccountRepository;
import com.payment.repository.TransactionRepository;
import com.payment.repository.FraudAlertRepository;
import com.payment.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PAYMENT SERVICE - Core business logic for payment processing
 * 
 * KEY CONCEPTS:
 * 1. @Transactional: All database operations are in one atomic transaction
 * 2. Idempotency: Duplicate requests are handled gracefully
 * 3. Pessimistic Locking: Prevents concurrent balance modifications
 * 4. Fraud Detection: Real-time anomaly detection
 * 5. Audit Trail: All transactions are logged for compliance
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditLogRepository auditLogRepository;
    
    @Value("${app.max-transaction-amount:100000}")
    private BigDecimal maxTransactionAmount;
    
    @Value("${app.fraud-detection.max-transactions-per-hour:10}")
    private int maxTransactionsPerHour;
    
    @Value("${app.fraud-detection.max-amount-per-hour:500000}")
    private BigDecimal maxAmountPerHour;
    
    /**
     * PROCESS PAYMENT - Main payment processing logic
     * 
     * FLOW:
     * 1. Check idempotency: Is this a duplicate request?
     * 2. Validate: Do accounts exist and have sufficient balance?
     * 3. Check fraud: Does transaction match fraud rules?
     * 4. Lock accounts: Prevent concurrent modifications
     * 5. Update balances: Atomically debit and credit
     * 6. Save transaction: Mark as completed
     * 7. Audit: Log the transaction
     * 
     * @Transactional ensures entire operation succeeds or fails atomically
     */
    @Transactional
    public TransactionResponse processPayment(TransactionRequest request, Long userId) {
        log.info("Processing payment for user {} with idempotency key: {}", 
                 userId, request.getIdempotencyKey());
        
        // STEP 1: IDEMPOTENCY CHECK
        // If we've seen this idempotencyKey before, return the previous result
        var existingTransaction = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTransaction.isPresent()) {
            log.info("Duplicate request detected. Returning previous transaction: {}", 
                     existingTransaction.get().getId());
            return mapToResponse(existingTransaction.get());
        }
        
        // STEP 2: VALIDATE ACCOUNTS AND BALANCE
        var fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
            .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        
        var toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
            .orElseThrow(() -> new IllegalArgumentException("To account not found"));
        
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        // STEP 3: FRAUD DETECTION
        detectFraud(fromAccount, request, userId);
        
        // STEP 4: LOCK ACCOUNTS - Pessimistic locking
        // This ensures only one transaction can modify these accounts at a time
        var lockedFromAccount = accountRepository.findByIdWithLock(fromAccount.getId())
            .orElseThrow(() -> new IllegalArgumentException("From account locked by another transaction"));
        
        var lockedToAccount = accountRepository.findByIdWithLock(toAccount.getId())
            .orElseThrow(() -> new IllegalArgumentException("To account locked by another transaction"));
        
        // STEP 5: UPDATE BALANCES - Atomic update
        // Debit from account
        lockedFromAccount.setBalance(lockedFromAccount.getBalance().subtract(request.getAmount()));
        // Credit to account
        lockedToAccount.setBalance(lockedToAccount.getBalance().add(request.getAmount()));
        
        accountRepository.save(lockedFromAccount);
        accountRepository.save(lockedToAccount);
        
        // STEP 6: SAVE TRANSACTION
        String referenceNumber = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = Transaction.builder()
            .idempotencyKey(request.getIdempotencyKey())
            .fromAccount(lockedFromAccount)
            .toAccount(lockedToAccount)
            .amount(request.getAmount())
            .status("COMPLETED")
            .description(request.getDescription())
            .referenceNumber(referenceNumber)
            .completedAt(LocalDateTime.now())
            .build();
        
        var savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction completed: {} with reference: {}", savedTransaction.getId(), referenceNumber);
        
        // STEP 7: AUDIT LOG
        auditLog(userId, "TRANSACTION_COMPLETED", "TRANSACTION", savedTransaction.getId(), 
                 null, referenceNumber, "Payment processed successfully");
        
        return mapToResponse(savedTransaction);
    }
    
    /**
     * FRAUD DETECTION - Real-time anomaly detection
     * 
     * Rules:
     * 1. Amount exceeds maximum
     * 2. Too many transactions in 1 hour
     * 3. Total amount in 1 hour exceeds limit
     */
    private void detectFraud(Account account, TransactionRequest request, Long userId) {
        String alertReason = null;
        String riskLevel = "LOW";
        
        // Rule 1: Amount check
        if (request.getAmount().compareTo(maxTransactionAmount) > 0) {
            alertReason = "Transaction amount exceeds maximum: " + request.getAmount();
            riskLevel = "HIGH";
        }
        
        // Rule 2 & 3: Velocity checks
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentTransactions = transactionRepository.countRecentTransactions(account.getId(), oneHourAgo);
        BigDecimal recentAmount = transactionRepository.sumRecentTransactionAmounts(account.getId(), oneHourAgo);
        
        if (recentTransactions > maxTransactionsPerHour) {
            alertReason = "Too many transactions in 1 hour: " + recentTransactions;
            riskLevel = "MEDIUM";
        }
        
        if (recentAmount.add(request.getAmount()).compareTo(maxAmountPerHour) > 0) {
            alertReason = "Transaction amount exceeds hourly limit. Total would be: " 
                         + recentAmount.add(request.getAmount());
            riskLevel = "HIGH";
        }
        
        // Create fraud alert if detected
        if (alertReason != null) {
            var fraudAlert = FraudAlert.builder()
                .user(account.getUser())
                .alertType("VELOCITY")
                .riskLevel(riskLevel)
                .status("OPEN")
                .reason(alertReason)
                .build();
            
            fraudAlertRepository.save(fraudAlert);
            log.warn("Fraud alert created for user {}: {}", userId, alertReason);
            
            // For HIGH risk, block transaction
            if ("HIGH".equals(riskLevel)) {
                throw new IllegalArgumentException("Transaction blocked due to fraud risk: " + alertReason);
            }
        }
    }
    
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .idempotencyKey(transaction.getIdempotencyKey())
            .fromAccountNumber(transaction.getFromAccount().getAccountNumber())
            .toAccountNumber(transaction.getToAccount().getAccountNumber())
            .amount(transaction.getAmount())
            .status(transaction.getStatus())
            .referenceNumber(transaction.getReferenceNumber())
            .description(transaction.getDescription())
            .failureReason(transaction.getFailureReason())
            .createdAt(transaction.getCreatedAt())
            .completedAt(transaction.getCompletedAt())
            .build();
    }
    
    private void auditLog(Long userId, String action, String entityType, Long entityId,
                         String oldValue, String newValue, String details) {
        var log = AuditLog.builder()
            .user(null) // userId will be set by controller context
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .oldValue(oldValue)
            .newValue(newValue)
            .details(details)
            .build();
        
        auditLogRepository.save(log);
    }
}
