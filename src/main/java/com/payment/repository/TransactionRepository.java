package com.payment.repository;

import com.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * IDEMPOTENCY CHECK
     * Before processing new transaction, check if idempotencyKey exists
     * If yes, return previous transaction (don't process again)
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find recent transactions for fraud detection velocity check
     * "How many transactions did this account make in last hour?"
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccount.id = :accountId AND t.createdAt > :since")
    long countRecentTransactions(Long accountId, LocalDateTime since);
    
    /**
     * Find recent transaction amounts for fraud detection
     * "How much money did this account send in last hour?"
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromAccount.id = :accountId AND t.createdAt > :since")
    java.math.BigDecimal sumRecentTransactionAmounts(Long accountId, LocalDateTime since);
    
    List<Transaction> findByFromAccountIdOrderByCreatedAtDesc(Long accountId);
    List<Transaction> findByToAccountIdOrderByCreatedAtDesc(Long accountId);
}
