package org.example.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.example.domain.issue.Priority;

/**
 * Partial update. {@code version} is the client's expected version for optimistic concurrency;
 * the other fields are null-means-unchanged (PATCH semantics).
 */
public record UpdateIssueRequest(
        @NotNull Long version,
        String title,
        String description,
        Priority priority,
        Integer storyPoints,
        UUID assigneeId,
        List<String> labels) {
}
