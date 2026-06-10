package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ACCOUNT RESPONSE DTO
 *
 * What we send back to the client after account operations.
 *
 * Notice: we do NOT include the User object or password.
 * DTOs act as a security boundary — only expose what the client needs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private Boolean active;
    private LocalDateTime createdAt;
}
