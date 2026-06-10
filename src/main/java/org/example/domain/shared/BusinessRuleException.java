package org.example.domain.shared;

/**
 * A domain invariant or policy was violated (e.g. a failed transition guard, an illegal sprint
 * state change, a WIP limit). Maps to HTTP 422.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
