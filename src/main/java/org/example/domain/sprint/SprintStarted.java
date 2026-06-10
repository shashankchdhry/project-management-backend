package org.example.domain.sprint;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

public record SprintStarted(
        UUID aggregateId,
        UUID projectId,
        String name,
        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "SprintStarted";
    }
}
