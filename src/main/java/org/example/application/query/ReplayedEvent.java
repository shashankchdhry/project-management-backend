package org.example.application.query;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** A single event in a replay stream (for WebSocket reconnection catch-up). */
public record ReplayedEvent(
        long seq,
        String type,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, Object> payload) {
}
