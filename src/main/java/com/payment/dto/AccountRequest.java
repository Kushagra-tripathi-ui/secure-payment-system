package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * ACCOUNT REQUEST DTO
 *
 * Data Transfer Object for creating a new account.
 *
 * accountType: What kind of account?
 *   - "SAVINGS" — interest-bearing, for long-term money
 *   - "CHECKING" — everyday transactions
 *   - "WALLET"  — digital wallet for fast transfers
 *
 * initialDeposit: Starting balance (optional, useful for testing).
 *   In production, this would link to a real funding source.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequest {

    @NotBlank(message = "Account type is required")
    @Pattern(
        regexp = "SAVINGS|CHECKING|WALLET",
        message = "Account type must be SAVINGS, CHECKING, or WALLET"
    )
    private String accountType;

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    private String notes;
}
