package org.example.application.port.out;

import java.util.List;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

/**
 * Appends domain events to the transactional outbox <em>within the current transaction</em>, so an
 * event is persisted iff the state change commits. The adapter assigns the
 * per-project sequence and serialises the payload.
 */
public interface EventOutboxPort {

    void append(UUID projectId, List<DomainEvent> events, String correlationId);
}
