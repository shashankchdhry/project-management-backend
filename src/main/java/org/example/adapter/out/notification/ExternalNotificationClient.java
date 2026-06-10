package org.example.adapter.out.notification;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Calls the (simulated) external notification service behind a Resilience4j circuit breaker.
 * When the service fails repeatedly the breaker opens and calls short-circuit to the
 * fallback, which persists the notification as PENDING for later delivery — board operations are
 * never blocked because this runs in the async outbox relay, decoupled from the request.
 */
@Component
@RequiredArgsConstructor
public class ExternalNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalNotificationClient.class);

    private final JdbcTemplate jdbc;

    @Value("${notification.simulate-failure:false}")
    private boolean simulateFailure;

    @CircuitBreaker(name = "notification", fallbackMethod = "queueForLater")
    public void send(NotificationMessage message) {
        if (simulateFailure) {
            throw new IllegalStateException("notification service unavailable");
        }
        // A real implementation would POST to the external service here.
        persist(message, "SENT");
    }

    /** Fallback (breaker open or call failed): queue the notification for delivery on recovery. */
    void queueForLater(NotificationMessage message, Throwable cause) {
        log.warn("Notification deferred (service degraded): {}", cause.getMessage());
        persist(message, "PENDING");
    }

    private void persist(NotificationMessage message, String status) {
        jdbc.update("""
                INSERT INTO notifications(id, recipient_id, type, issue_id, payload, status)
                VALUES (?,?,?,?, ?::jsonb, ?)
                """,
                UUID.randomUUID(), message.recipientId(), message.type(), message.issueId(),
                message.payloadJson(), status);
    }
}
