package org.example.application.query;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** One entry in a project's activity feed (a row of the append-only audit trail). */
public record ActivityEntry(
        UUID id,
        UUID issueId,
        UUID actorId,
        String eventType,
        String summary,
        Map<String, Object> changes,
        String correlationId,
        Instant createdAt) {
}
