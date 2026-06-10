package org.example.application.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** A persisted outbox event, handed to {@link DomainEventHandler}s by the relay. */
public record DomainEventRecord(
        UUID eventId,
        UUID projectId,
        long seq,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        Map<String, Object> payload,
        String correlationId,
        Instant occurredAt) {
}
