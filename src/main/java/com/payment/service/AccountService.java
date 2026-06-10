package com.payment.service;

import com.payment.dto.AccountRequest;
import com.payment.dto.AccountResponse;
import com.payment.entity.Account;
import com.payment.entity.User;
import com.payment.exception.ResourceNotFoundException;
import com.payment.repository.AccountRepository;
import com.payment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * ACCOUNT SERVICE - Business logic for managing bank accounts
 *
 * CONCEPT: Why do users need accounts?
 *
 * In our payment system:
 * - User = the person (email, password, name)
 * - Account = the wallet/bank account where money is stored
 * - One user can have multiple accounts (like savings + checking)
 *
 * Transactions move money between accounts, not between users directly.
 * This mirrors how real banks work.
 *
 * @Transactional: ensures DB operations are atomic
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * CREATE ACCOUNT for a user
     *
     * Generates a unique 12-digit account number automatically.
     * Initial balance can be set by the request (useful for testing).
     * In a real system, the initial deposit would come from a verified funding source.
     */
    @Transactional
    public AccountResponse createAccount(AccountRequest request, Long userId) {
        log.info("Creating account for user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Generate unique account number (12 digits)
        String accountNumber = generateAccountNumber();

        // Ensure uniqueness — regenerate if collision
        while (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            accountNumber = generateAccountNumber();
        }

        Account account = Account.builder()
            .user(user)
            .accountNumber(accountNumber)
            .accountType(request.getAccountType())
            .balance(request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO)
            .active(true)
            .notes(request.getNotes())
            .build();

        Account saved = accountRepository.save(account);
        log.info("Account created: {} for user: {}", saved.getAccountNumber(), userId);

        return mapToResponse(saved);
    }

    /**
     * GET ALL ACCOUNTS for a user
     *
     * Returns all accounts belonging to the logged-in user.
     * We filter by userId so users can only see their own accounts.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(Long userId) {
        log.debug("Fetching accounts for user: {}", userId);
        return accountRepository.findByUserId(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * GET SINGLE ACCOUNT by account number
     *
     * Security check: ensures the account belongs to the requesting user.
     * Prevents user A from viewing user B's balance.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber, Long userId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        // Security: ensure the account belongs to this user
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Account does not belong to you");
        }

        return mapToResponse(account);
    }

    /**
     * GENERATE ACCOUNT NUMBER
     *
     * Format: "PAY" + 9 random digits = "PAY123456789"
     * The PAY prefix makes it easy to identify accounts in logs/support.
     */
    private String generateAccountNumber() {
        Random random = new Random();
        long number = (long) (random.nextDouble() * 900_000_000L) + 100_000_000L;
        return "PAY" + number;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
            .id(account.getId())
            .accountNumber(account.getAccountNumber())
            .accountType(account.getAccountType())
            .balance(account.getBalance())
            .active(account.getActive())
            .createdAt(account.getCreatedAt())
            .build();
    }
}
