package org.example.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots. Aggregates record {@link DomainEvent}s as they mutate; the
 * application layer pulls them after a successful transaction to write to the outbox.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Returns and clears the recorded events (called once after the change is persisted). */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    /** Read-only view of currently recorded events (for assertions/tests). */
    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
}