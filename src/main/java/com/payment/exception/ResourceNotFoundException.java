package com.payment.exception;

/**
 * RESOURCE NOT FOUND EXCEPTION
 *
 * CONCEPT: Custom Exceptions for cleaner code
 *
 * Why create a custom exception instead of using IllegalArgumentException for everything?
 * - Semantic clarity: "Not Found" (404) vs "Bad Request" (400) are different HTTP meanings
 * - GlobalExceptionHandler can map each exception type to the correct HTTP status
 * - Makes service code more readable: "throw new ResourceNotFoundException" is self-explanatory
 *
 * Example usages:
 *   throw new ResourceNotFoundException("User not found with id: " + id);
 *   throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor: "Account not found with accountNumber: ACC123"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}
