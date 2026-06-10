package org.example.domain.workflow;

import java.util.UUID;

/**
 * A configurable status (board column) within a {@link Workflow}.
 *
 * @param wipLimit max issues allowed in this column, or {@code null} for unlimited
 */
public record WorkflowStatus(UUID id, String name, StatusCategory category, int position, Integer wipLimit) {

    public boolean hasWipLimit() {
        return wipLimit != null;
    }

    public boolean isDone() {
        return category == StatusCategory.DONE;
    }
}
