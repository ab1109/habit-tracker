package com.habittracker.common.domain;

/**
 * Marks a violation of a domain invariant (invalid input the domain layer itself
 * rejected), as opposed to a framework-level validation failure. The API layer
 * maps this to a 400 response without needing to know which module threw it.
 */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
