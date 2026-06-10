package org.example.domain.issue;

import java.util.List;
import java.util.UUID;

/**
 * A partial update to an issue's editable fields. A {@code null} field means "leave unchanged";
 * this models PATCH semantics. Applied by {@link Issue#updateDetails(IssueDetailsUpdate)}.
 */
public record IssueDetailsUpdate(
        String title,
        String description,
        Priority priority,
        Integer storyPoints,
        UUID assigneeId,
        List<String> labels) {
}
