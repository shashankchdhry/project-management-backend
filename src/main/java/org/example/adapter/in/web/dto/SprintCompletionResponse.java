package org.example.adapter.in.web.dto;

import java.util.List;
import java.util.UUID;
import org.example.domain.sprint.SprintCompletionResult;

public record SprintCompletionResponse(
        int velocity,
        List<UUID> incompleteIssueIds,
        List<UUID> carriedOverIssueIds) {

    public static SprintCompletionResponse from(SprintCompletionResult r) {
        return new SprintCompletionResponse(r.velocity(), r.incompleteIssueIds(), r.carriedOverIssueIds());
    }
}
