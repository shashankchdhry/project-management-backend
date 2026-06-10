package org.example.application.command;

import java.util.Set;
import java.util.UUID;

/**
 * @param carryOverIssueIds incomplete issues to move forward
 * @param targetSprintId    destination sprint for carry-over, or {@code null} for the backlog
 */
public record CompleteSprintCommand(Set<UUID> carryOverIssueIds, UUID targetSprintId) {

    public CompleteSprintCommand {
        carryOverIssueIds = carryOverIssueIds == null ? Set.of() : Set.copyOf(carryOverIssueIds);
    }
}
