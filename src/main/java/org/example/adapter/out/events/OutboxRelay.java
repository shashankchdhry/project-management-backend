package org.example.adapter.out.events;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.repository.DomainEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the transactional outbox and relays unpublished events to handlers. Each event is dispatched
 * in its own transaction, so a single failing handler doesn't block the rest — it just retries next
 * poll. Public {@link #poll()} so tests can drive it deterministically.
 */
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH = 200;

    private final DomainEventJpaRepository events;
    private final OutboxDispatcher dispatcher;

    @Scheduled(fixedDelayString = "${outbox.relay.delay-ms:1000}")
    public void poll() {
        try {
            for (UUID id : events.findUnpublishedIds(PageRequest.of(0, BATCH))) {
                try {
                    dispatcher.dispatch(id);
                } catch (Exception ex) {
                    log.warn("Outbox relay failed for event {}: {}", id, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            // e.g. a transient DB blip while fetching; self-heals on the next poll
            log.warn("Outbox relay poll failed: {}", ex.getMessage());
        }
    }
}
