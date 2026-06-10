package org.example.adapter.out.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.event.DomainEventHandler;
import org.example.application.event.DomainEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns domain events into notifications (here: notify the reporter when an issue is created).
 * Best-effort: any failure is swallowed so a notification problem can never break the outbox relay
 * — delivery resilience is the circuit breaker's job ({@link ExternalNotificationClient}).
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatcher implements DomainEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final ExternalNotificationClient client;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(DomainEventRecord event) {
        if (!"IssueCreated".equals(event.eventType())) {
            return;
        }
        Object reporter = event.payload().get("reporterId");
        if (reporter == null) {
            return;
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(event.payload());
            client.send(new NotificationMessage(UUID.fromString(reporter.toString()),
                    "ISSUE_CREATED", event.aggregateId(), payloadJson));
        } catch (Exception ex) {
            log.warn("Notification skipped for event {}: {}", event.eventId(), ex.getMessage());
        }
    }
}
