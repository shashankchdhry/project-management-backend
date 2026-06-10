package org.example.domain.sprint;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of completing a sprint: the achieved velocity, which issues were incomplete, and which of
 * those were carried over. Surfaced in the sprint-complete API response.
 */
public record SprintCompletionResult(
        int velocity,
        List<UUID> incompleteIssueIds,
        List<UUID> carriedOverIssueIds) {

    public SprintCompletionResult {
        incompleteIssueIds = List.copyOf(incompleteIssueIds);
        carriedOverIssueIds = List.copyOf(carriedOverIssueIds);
    }
}
