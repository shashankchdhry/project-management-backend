package org.example.adapter.out.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.application.event.DomainEventHandler;
import org.example.application.event.DomainEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes each relayed event to a per-project Redis channel, from which every app instance's
 * {@code RedisMessageListenerContainer} forwards it to local WebSocket subscribers — giving
 * cross-instance fan-out. Fails soft: if Redis is unavailable the broadcast is
 * dropped (clients recover via replay) and the relay still completes.
 */
@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher implements DomainEventHandler {

    public static final String CHANNEL_PREFIX = "rt:board:";
    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(DomainEventRecord event) {
        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("type", event.eventType());
            message.put("projectId", event.projectId().toString());
            message.put("seq", event.seq());
            message.put("aggregateId", event.aggregateId().toString());
            message.put("occurredAt", event.occurredAt().toString());
            message.put("data", event.payload());
            redis.convertAndSend(CHANNEL_PREFIX + event.projectId(), objectMapper.writeValueAsString(message));
        } catch (Exception ex) {
            log.warn("Real-time broadcast dropped for event {}: {}", event.eventId(), ex.getMessage());
        }
    }
}
