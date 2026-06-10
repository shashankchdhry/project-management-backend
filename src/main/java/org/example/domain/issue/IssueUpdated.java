package org.example.domain.issue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;

/**
 * @param changes field name → {@code {"old": ..., "new": ...}} (feeds the activity log / audit trail)
 */
public record IssueUpdated(
        UUID aggregateId,
        String issueKey,
        Map<String, Object> changes,
        Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "IssueUpdated";
    }
}
