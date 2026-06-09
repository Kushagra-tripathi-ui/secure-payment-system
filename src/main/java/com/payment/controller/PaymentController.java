package com.payment.controller;

import com.payment.dto.TransactionRequest;
import com.payment.dto.TransactionResponse;
import com.payment.dto.ApiResponse;
import com.payment.service.PaymentService;
import com.payment.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * PAYMENT CONTROLLER - REST endpoints for payment processing
 * 
 * API Endpoints:
 * POST /api/payments/process - Process a payment transaction
 * 
 * Security:
 * - @PreAuthorize: Only authenticated users can access
 * - @CurrentUser: Injects current user's ID from JWT token
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> processPayment(
            @Valid @RequestBody TransactionRequest request,
            @CurrentUser Long userId) {
        
        log.info("Payment request received from user: {}", userId);
        
        try {
            TransactionResponse response = paymentService.processPayment(request, userId);
            return ResponseEntity.ok(
                ApiResponse.success("Payment processed successfully", response)
            );
        } catch (IllegalArgumentException e) {
            log.warn("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Payment processing failed"));
        }
    }
}
