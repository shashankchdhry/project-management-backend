package org.example.domain.issue;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

public record IssueCreated(
        UUID aggregateId,
        String issueKey,
        UUID projectId,
        IssueType type,
        String title,
        UUID statusId,
        UUID reporterId,
        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "IssueCreated";
    }
}
