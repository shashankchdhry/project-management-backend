package org.example.domain.workflow;

import java.util.Map;
import java.util.UUID;

/**
 * An allowed move between statuses. A {@code null} {@link #fromStatusId()} marks the initial
 * transition (the status new issues enter).
 *
 * @param guard      declarative pre-conditions, e.g. {@code {"requireAssignee": true}}
 * @param postAction declarative side effects, e.g. {@code {"assignReviewer": "ROUND_ROBIN"}}
 */
public record WorkflowTransition(
        UUID id,
        String name,
        UUID fromStatusId,
        UUID toStatusId,
        Map<String, Object> guard,
        Map<String, Object> postAction) {

    public boolean isInitial() {
        return fromStatusId == null;
    }
}
