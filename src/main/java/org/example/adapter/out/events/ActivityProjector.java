package org.example.adapter.out.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.event.DomainEventHandler;
import org.example.application.event.DomainEventRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Projects domain events into the append-only activity_log / audit trail. */
@Component
@RequiredArgsConstructor
public class ActivityProjector implements DomainEventHandler {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(DomainEventRecord event) {
        UUID issueId = "ISSUE".equals(event.aggregateType()) ? event.aggregateId() : null;
        jdbc.update("""
                INSERT INTO activity_log(id, project_id, issue_id, actor_id, event_type, summary, changes, correlation_id)
                VALUES (?,?,?,?,?,?, ?::jsonb, ?)
                """,
                UUID.randomUUID(), event.projectId(), issueId, null, event.eventType(),
                summarize(event), changesJson(event), event.correlationId());
    }

    private String summarize(DomainEventRecord e) {
        Map<String, Object> p = e.payload();
        return switch (e.eventType()) {
            case "IssueCreated" -> "Created " + p.get("issueKey") + " — " + p.get("title");
            case "IssueUpdated" -> "Updated " + p.get("issueKey");
            case "StatusChanged" -> p.get("issueKey") + ": " + p.get("fromStatus") + " → " + p.get("toStatus");
            case "IssueMovedToSprint" -> p.get("issueKey") + " moved to sprint";
            case "SprintStarted" -> "Sprint started: " + p.get("name");
            case "SprintCompleted" -> "Sprint completed (velocity " + p.get("velocity") + ")";
            default -> e.eventType();
        };
    }

    private String changesJson(DomainEventRecord e) {
        Object changes = e.payload().get("changes");
        if (changes == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (Exception ex) {
            return null;
        }
    }
}
