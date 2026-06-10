package org.example.domain.shared;

import java.util.List;

/**
 * Raised when an issue cannot move to a requested status because no transition exists from its
 * current status. Carries the names of the transitions that <em>are</em> allowed, so the web
 * adapter can return them in the 422 body.
 */
public class WorkflowTransitionException extends DomainException {

    private final List<String> allowedTransitions;

    public WorkflowTransitionException(String message, List<String> allowedTransitions) {
        super(message);
        this.allowedTransitions = List.copyOf(allowedTransitions);
    }

    public List<String> allowedTransitions() {
        return allowedTransitions;
    }
}
