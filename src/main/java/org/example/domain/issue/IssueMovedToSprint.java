package org.example.domain.issue;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

/**
 * @param sprintId the destination sprint, or {@code null} when moved back to the backlog
 */
public record IssueMovedToSprint(
        UUID aggregateId,
        String issueKey,
        UUID sprintId,
        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "IssueMovedToSprint";
    }
}
