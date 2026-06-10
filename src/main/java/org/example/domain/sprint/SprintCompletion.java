package org.example.domain.sprint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.example.domain.issue.Issue;
import org.example.domain.shared.BusinessRuleException;

/**
 * Domain service for completing a sprint. It spans the sprint and its issues, so it
 * lives outside the {@link Sprint} aggregate. The application layer runs this under an advisory
 * lock so concurrent completes can't double-apply carry-over.
 */
public final class SprintCompletion {

    /**
     * Completes {@code sprint}: computes velocity from DONE issues, identifies incomplete issues,
     * carries over the selected ones to {@code targetSprintId} (or backlog when {@code null}), and
     * closes the sprint.
     *
     * @param doneStatusIds     status ids whose category is DONE
     * @param carryOverIssueIds which incomplete issues to move forward
     */
    public SprintCompletionResult complete(Sprint sprint, List<Issue> sprintIssues, Set<UUID> doneStatusIds,
                                           Set<UUID> carryOverIssueIds, UUID targetSprintId, Instant when) {
        if (sprint.state() != SprintState.ACTIVE) {
            throw new BusinessRuleException("Only an ACTIVE sprint can be completed (current state: " + sprint.state() + ")");
        }

        List<UUID> incomplete = new ArrayList<>();
        List<UUID> carried = new ArrayList<>();
        int velocity = 0;

        for (Issue issue : sprintIssues) {
            if (doneStatusIds.contains(issue.statusId())) {
                velocity += issue.storyPoints() == null ? 0 : issue.storyPoints();
            } else {
                incomplete.add(issue.id());
                if (carryOverIssueIds.contains(issue.id())) {
                    issue.moveToSprint(targetSprintId);
                    carried.add(issue.id());
                }
            }
        }

        sprint.markCompleted(velocity, when);
        return new SprintCompletionResult(velocity, incomplete, carried);
    }
}
