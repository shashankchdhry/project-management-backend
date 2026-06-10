package org.example.domain.shared;

/**
 * Base type for all domain rule violations. The web adapter maps subclasses to HTTP responses:
 * business rules → 422, etc. Domain code never references HTTP.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
