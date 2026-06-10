package org.example.domain.sprint;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

public record SprintCompleted(
        UUID aggregateId,
        UUID projectId,
        int velocity,
        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "SprintCompleted";
    }
}
