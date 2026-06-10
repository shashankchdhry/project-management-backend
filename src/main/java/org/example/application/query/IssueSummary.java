package org.example.application.query;

import java.time.Instant;
import java.util.UUID;

/** A single issue in a search/list result. */
public record IssueSummary(
        UUID id,
        String key,
        String type,
        String title,
        UUID statusId,
        String statusName,
        String priority,
        UUID assigneeId,
        Integer storyPoints,
        Instant createdAt,
        Instant updatedAt) {
}
