package com.payment.service;

import com.payment.dto.TransactionRequest;
import com.payment.dto.TransactionResponse;
import com.payment.entity.Account;
import com.payment.entity.AuditLog;
import com.payment.entity.FraudAlert;
import com.payment.entity.Transaction;
import com.payment.entity.User;
import com.payment.exception.ResourceNotFoundException;
import com.payment.repository.AccountRepository;
import com.payment.repository.AuditLogRepository;
import com.payment.repository.FraudAlertRepository;
import com.payment.repository.TransactionRepository;
import com.payment.repository.UserRepository;
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
 * 1. @Transactional: All database operations are in one atomic transaction (ACID)
 * 2. Idempotency: Duplicate requests with same idempotencyKey return previous result
 * 3. Pessimistic Locking: SELECT FOR UPDATE prevents concurrent balance modifications
 * 4. Fraud Detection: Real-time rules engine flags suspicious transactions
 * 5. Audit Trail: Every action is logged with user, timestamp, old/new values
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Value("${app.max-transaction-amount:100000}")
    private BigDecimal maxTransactionAmount;

    @Value("${app.fraud-detection.max-transactions-per-hour:10}")
    private int maxTransactionsPerHour;

    @Value("${app.fraud-detection.max-amount-per-hour:500000}")
    private BigDecimal maxAmountPerHour;

    /**
     * PROCESS PAYMENT - Main payment processing method
     *
     * FLOW:
     * 1. Idempotency check     → don't process the same payment twice
     * 2. Account validation    → accounts must exist and be active
     * 3. Balance check         → sufficient funds?
     * 4. Fraud detection       → does this look suspicious?
     * 5. Pessimistic locking   → lock both accounts so nobody else touches them
     * 6. Balance update        → debit sender, credit receiver
     * 7. Save transaction      → record what happened
     * 8. Audit log             → write compliance trail
     *
     * @Transactional: If ANY step fails, ALL database changes roll back.
     * This ensures money is never lost — partial updates are impossible.
     */
    @Transactional
    public TransactionResponse processPayment(TransactionRequest request, Long userId) {
        log.info("Processing payment for user {} with idempotency key: {}",
                userId, request.getIdempotencyKey());

        // ─── STEP 1: IDEMPOTENCY CHECK ───────────────────────────────────────
        // If we already processed a request with this idempotencyKey,
        // return the same result without processing again.
        // This safely handles: network retries, button double-clicks, timeout replays.
        var existingTransaction = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTransaction.isPresent()) {
            log.info("Duplicate request detected. Returning existing transaction: {}",
                    existingTransaction.get().getId());
            return mapToResponse(existingTransaction.get());
        }

        // ─── STEP 2: VALIDATE ACCOUNTS ───────────────────────────────────────
        var fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.getFromAccountNumber()));

        var toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.getToAccountNumber()));

        if (!fromAccount.getActive()) {
            throw new IllegalArgumentException("Source account is inactive");
        }
        if (!toAccount.getActive()) {
            throw new IllegalArgumentException("Destination account is inactive");
        }
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // ─── STEP 3: BALANCE CHECK ────────────────────────────────────────────
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient balance. Available: %.2f, Requested: %.2f",
                    fromAccount.getBalance(), request.getAmount()));
        }

        // ─── STEP 4: FRAUD DETECTION ──────────────────────────────────────────
        // Run fraud rules before locking. If HIGH risk, throws exception and
        // the @Transactional rolls back — no partial state is saved.
        detectFraud(fromAccount, request, userId);

        // ─── STEP 5: PESSIMISTIC LOCKING ──────────────────────────────────────
        // SELECT ... FOR UPDATE on both accounts.
        // Any other transaction trying to touch these accounts will wait until we're done.
        // This is the database-level guarantee for ACID compliance.
        var lockedFrom = accountRepository.findByIdWithLock(fromAccount.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", fromAccount.getId()));
        var lockedTo = accountRepository.findByIdWithLock(toAccount.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", toAccount.getId()));

        // Re-check balance after locking (balance may have changed between step 3 and 5)
        if (lockedFrom.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance after lock");
        }

        // ─── STEP 6: UPDATE BALANCES ──────────────────────────────────────────
        BigDecimal oldFromBalance = lockedFrom.getBalance();
        BigDecimal oldToBalance   = lockedTo.getBalance();

        lockedFrom.setBalance(lockedFrom.getBalance().subtract(request.getAmount()));
        lockedTo.setBalance(lockedTo.getBalance().add(request.getAmount()));

        accountRepository.save(lockedFrom);
        accountRepository.save(lockedTo);

        // ─── STEP 7: SAVE TRANSACTION ─────────────────────────────────────────
        String referenceNumber = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
            .idempotencyKey(request.getIdempotencyKey())
            .fromAccount(lockedFrom)
            .toAccount(lockedTo)
            .amount(request.getAmount())
            .status("COMPLETED")
            .description(request.getDescription())
            .referenceNumber(referenceNumber)
            .completedAt(LocalDateTime.now())
            .build();

        var saved = transactionRepository.save(transaction);
        log.info("Transaction completed: {} | ref: {}", saved.getId(), referenceNumber);

        // ─── STEP 8: AUDIT LOG ────────────────────────────────────────────────
        // Compliance requirement: log every successful payment with before/after balances.
        User actor = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        saveAuditLog(
            actor,
            "TRANSACTION_COMPLETED",
            "TRANSACTION",
            saved.getId(),
            "from_balance=" + oldFromBalance + ",to_balance=" + oldToBalance,
            "from_balance=" + lockedFrom.getBalance() + ",to_balance=" + lockedTo.getBalance(),
            "Payment of " + request.getAmount() + " | ref: " + referenceNumber
        );

        return mapToResponse(saved);
    }

    /**
     * FRAUD DETECTION ENGINE
     *
     * Three rules checked in order of severity:
     *
     * Rule 1 — AMOUNT: Single transaction exceeds the configured maximum.
     *   Risk: HIGH → transaction blocked immediately.
     *
     * Rule 2 — VELOCITY (count): More than N transactions in the last hour.
     *   Risk: MEDIUM → alert raised, transaction allowed.
     *   Reason: Could be automated fraud, not necessarily harmful by itself.
     *
     * Rule 3 — VELOCITY (amount): Cumulative sent amount in last hour exceeds limit.
     *   Risk: HIGH → transaction blocked.
     *   Reason: Card testing or money laundering pattern.
     *
     * FraudAlert is saved to DB for every rule violation so a compliance
     * officer can review it later. HIGH risk also throws an exception
     * which causes @Transactional to roll back the entire payment.
     */
    private void detectFraud(Account account, TransactionRequest request, Long userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        long recentCount  = transactionRepository.countRecentTransactions(account.getId(), oneHourAgo);
        BigDecimal recentAmt = transactionRepository.sumRecentTransactionAmounts(account.getId(), oneHourAgo);

        String alertReason = null;
        String alertType   = "AMOUNT";
        String riskLevel   = "LOW";

        // Rule 1: Single transaction too large
        if (request.getAmount().compareTo(maxTransactionAmount) > 0) {
            alertReason = "Transaction amount " + request.getAmount() +
                          " exceeds maximum " + maxTransactionAmount;
            alertType = "AMOUNT";
            riskLevel = "HIGH";
        }

        // Rule 2: Velocity — too many transactions
        if (alertReason == null && recentCount >= maxTransactionsPerHour) {
            alertReason = "Velocity: " + recentCount + " transactions in the last hour (limit: " + maxTransactionsPerHour + ")";
            alertType = "VELOCITY";
            riskLevel = "MEDIUM";
        }

        // Rule 3: Velocity — total amount too high
        BigDecimal projectedTotal = recentAmt.add(request.getAmount());
        if (projectedTotal.compareTo(maxAmountPerHour) > 0) {
            alertReason = "Hourly transfer limit exceeded. Projected total: " + projectedTotal +
                          " (limit: " + maxAmountPerHour + ")";
            alertType = "VELOCITY";
            riskLevel = "HIGH";
        }

        if (alertReason != null) {
            User user = account.getUser();

            FraudAlert alert = FraudAlert.builder()
                .user(user)
                .alertType(alertType)
                .riskLevel(riskLevel)
                .status("OPEN")
                .reason(alertReason)
                .build();

            fraudAlertRepository.save(alert);
            log.warn("Fraud alert [{}][{}] for user {}: {}", alertType, riskLevel, userId, alertReason);

            if ("HIGH".equals(riskLevel)) {
                throw new IllegalArgumentException("Transaction blocked — fraud risk detected: " + alertReason);
            }
        }
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
            .id(t.getId())
            .idempotencyKey(t.getIdempotencyKey())
            .fromAccountNumber(t.getFromAccount().getAccountNumber())
            .toAccountNumber(t.getToAccount().getAccountNumber())
            .amount(t.getAmount())
            .status(t.getStatus())
            .referenceNumber(t.getReferenceNumber())
            .description(t.getDescription())
            .failureReason(t.getFailureReason())
            .createdAt(t.getCreatedAt())
            .completedAt(t.getCompletedAt())
            .build();
    }

    private void saveAuditLog(User user, String action, String entityType, Long entityId,
                              String oldValue, String newValue, String details) {
        AuditLog auditLog = AuditLog.builder()
            .user(user)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .oldValue(oldValue)
            .newValue(newValue)
            .details(details)
            .build();

        auditLogRepository.save(auditLog);
    }
}
