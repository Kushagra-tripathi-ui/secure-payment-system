package com.payment.exception;

import com.payment.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER - Centralized error handling for the entire app
 *
 * CONCEPT: @RestControllerAdvice
 *
 * Without this:
 *   - Validation errors return a huge ugly JSON with Spring's default fields
 *   - RuntimeExceptions return 500 with a stack trace
 *   - Every controller needs its own try/catch blocks
 *
 * With this:
 *   - One place handles ALL exceptions across ALL controllers
 *   - Clean, consistent error responses: { success: false, message: "..." }
 *   - Correct HTTP status codes (400 for bad input, 404 for not found, etc.)
 *
 * Spring automatically calls the right @ExceptionHandler method
 * based on what exception was thrown.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * VALIDATION ERRORS (@Valid failures)
     *
     * When a request body fails @NotBlank, @Email, @Size etc. checks,
     * Spring throws MethodArgumentNotValidException.
     *
     * We extract the field-level messages and return them clearly:
     * { "email": "Email should be valid", "password": "Password is required" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Validation failed: " + errors));
    }

    /**
     * BUSINESS LOGIC ERRORS (IllegalArgumentException)
     *
     * We use IllegalArgumentException throughout services for:
     * - "Email already registered"
     * - "Insufficient balance"
     * - "Account not found"
     * - "Transaction blocked due to fraud risk"
     *
     * These are BAD REQUEST (400) — the caller sent bad data.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Business logic error: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * RESOURCE NOT FOUND (ResourceNotFoundException)
     *
     * When something doesn't exist — account, user, transaction.
     * Returns 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * OPTIMISTIC LOCKING FAILURES
     *
     * When two concurrent requests try to modify the same account,
     * one will fail with ObjectOptimisticLockingFailureException.
     * We catch it and return a user-friendly message.
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse> handleOptimisticLock(Exception ex) {
        log.warn("Concurrent modification detected, please retry: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("Transaction conflict detected. Please retry."));
    }

    /**
     * CATCH-ALL for unexpected errors
     *
     * Anything we didn't predict — database down, NullPointerException, etc.
     * Returns 500 INTERNAL SERVER ERROR.
     * We log the full stack trace for debugging but don't expose it to users.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred. Please try again."));
    }
}
