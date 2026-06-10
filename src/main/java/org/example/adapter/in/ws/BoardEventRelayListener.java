package org.example.adapter.in.ws;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.realtime.RealtimeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges Redis pub/sub → local STOMP sessions: receives a board event published by any instance and
 * forwards it to the project's board topic for clients connected to <em>this</em> instance.
 *
 * <p>The event is forwarded as the raw JSON string (a text frame) — clients {@code JSON.parse} it.
 */
@Component
@RequiredArgsConstructor
public class BoardEventRelayListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(BoardEventRelayListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String projectId = channel.substring(RealtimeEventPublisher.CHANNEL_PREFIX.length());
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/board", body);
        } catch (Exception ex) {
            log.warn("Failed to relay realtime message to STOMP: {}", ex.getMessage());
        }
    }
}
