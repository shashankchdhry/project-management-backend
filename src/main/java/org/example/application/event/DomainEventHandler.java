package org.example.application.event;

/**
 * A consumer of relayed domain events. Implementations (activity feed, board projection,
 * notifications, WebSocket broadcast) are invoked by the outbox relay within its transaction.
 * All beans implementing this are auto-discovered and dispatched to.
 */
public interface DomainEventHandler {

    void handle(DomainEventRecord event);
}
