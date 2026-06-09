package com.payment.repository;

import com.payment.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    /**
     * PESSIMISTIC LOCKING for Account
     * 
     * CONCEPT: Two approaches to handle concurrent updates
     * 
     * 1. OPTIMISTIC: Use @Version column (already in Account)
     *    - Read data
     *    - Modify data
     *    - Update with version check
     *    - If version changed, fail and retry
     *    Good for: Low conflict scenarios
     * 
     * 2. PESSIMISTIC: Lock row immediately
     *    - Lock row in database
     *    - Read data
     *    - Modify data
     *    - Release lock
     *    - No other transaction can access locked row
     *    Good for: High conflict scenarios, critical operations
     * 
     * For payment balance updates, we use PESSIMISTIC_WRITE
     * This ensures only one thread modifies balance at a time
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(Long id);
    
    List<Account> findByUserId(Long userId);
}
