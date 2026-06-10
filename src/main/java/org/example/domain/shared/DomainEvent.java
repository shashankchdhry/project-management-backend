package org.example.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * A fact that happened in the domain. Events are raised by aggregates, collected by
 * {@link AggregateRoot}, and (in later layers) persisted to the transactional outbox and relayed
 * to projectors, notifications, and WebSocket clients.
 *
 * <p>Implementations are immutable records carrying only the data a consumer needs.
 */
public interface DomainEvent {

    UUID aggregateId();

    Instant occurredAt();

    /** Stable type name, e.g. {@code "IssueCreated"} — used as the persisted event_type. */
    String eventType();
}