package org.example.application.shared;

import org.example.domain.issue.Issue;

/**
 * The client's expected version did not match the current version of the issue.
 * Carries the current aggregate so the web layer can return it in the 409 body for client merge.
 */
public class OptimisticLockConflictException extends RuntimeException {

    private final transient Issue current;

    public OptimisticLockConflictException(Issue current, long expectedVersion) {
        super("Issue " + current.key() + " is at version " + current.version()
                + "; you submitted version " + expectedVersion);
        this.current = current;
    }

    public Issue current() {
        return current;
    }
}
