package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String idempotencyKey;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String status;
    private String referenceNumber;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
