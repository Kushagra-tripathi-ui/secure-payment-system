package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTOs (Data Transfer Objects) - Objects for API communication
 * 
 * Why separate DTOs from Entities?
 * 1. Security: Don't expose internal password/version fields
 * 2. Flexibility: API shape independent of database schema
 * 3. Validation: Add @Valid constraints for input validation
 * 4. API Evolution: Change API without changing database
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
    
    @NotNull(message = "From account number is required")
    @NotBlank(message = "From account number cannot be blank")
    private String fromAccountNumber;
    
    @NotNull(message = "To account number is required")
    @NotBlank(message = "To account number cannot be blank")
    private String toAccountNumber;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000.00", message = "Amount exceeds maximum limit")
    private BigDecimal amount;
    
    @NotNull(message = "Idempotency key is required")
    @NotBlank(message = "Idempotency key cannot be blank")
    @Size(min = 10, max = 100, message = "Idempotency key must be 10-100 characters")
    private String idempotencyKey;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
