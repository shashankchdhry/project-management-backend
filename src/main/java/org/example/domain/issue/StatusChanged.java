package org.example.domain.issue;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

public record StatusChanged(
        UUID aggregateId,
        String issueKey,
        UUID fromStatusId,
        String fromStatus,
        UUID toStatusId,
        String toStatus,
        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "StatusChanged";
    }
}
