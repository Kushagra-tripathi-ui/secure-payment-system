package com.payment.controller;

import com.payment.dto.AccountRequest;
import com.payment.dto.AccountResponse;
import com.payment.dto.ApiResponse;
import com.payment.security.CurrentUser;
import com.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * ACCOUNT CONTROLLER - REST endpoints for account management
 *
 * API Endpoints:
 *   POST   /api/accounts          - Create a new account
 *   GET    /api/accounts          - Get all my accounts
 *   GET    /api/accounts/{number} - Get a specific account by account number
 *
 * All endpoints require authentication (@PreAuthorize).
 * @CurrentUser Long userId automatically injects the logged-in user's ID
 * from the JWT token — no need to pass it manually in the request body.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    /**
     * CREATE ACCOUNT
     *
     * POST /api/accounts
     * Body: { "accountType": "SAVINGS", "initialDeposit": 5000.00 }
     *
     * Returns 201 CREATED with the new account details.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> createAccount(
            @Valid @RequestBody AccountRequest request,
            @CurrentUser Long userId) {

        log.info("Create account request from user: {}", userId);
        AccountResponse response = accountService.createAccount(request, userId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Account created successfully", response));
    }

    /**
     * GET MY ACCOUNTS
     *
     * GET /api/accounts
     * Returns a list of all accounts belonging to the logged-in user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getMyAccounts(@CurrentUser Long userId) {
        log.debug("Fetching accounts for user: {}", userId);
        List<AccountResponse> accounts = accountService.getMyAccounts(userId);
        return ResponseEntity.ok(ApiResponse.success("Accounts fetched successfully", accounts));
    }

    /**
     * GET ACCOUNT BY NUMBER
     *
     * GET /api/accounts/PAY123456789
     * Returns balance and details for a specific account.
     * Security: only the owner can view their account.
     */
    @GetMapping("/{accountNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getAccount(
            @PathVariable String accountNumber,
            @CurrentUser Long userId) {

        AccountResponse response = accountService.getAccount(accountNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("Account fetched successfully", response));
    }
}
