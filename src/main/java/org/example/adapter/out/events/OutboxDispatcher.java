package org.example.adapter.out.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.entity.DomainEventEntity;
import org.example.adapter.out.persistence.repository.DomainEventJpaRepository;
import org.example.application.event.DomainEventHandler;
import org.example.application.event.DomainEventRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dispatches one outbox event to all handlers and marks it published — each in its own transaction. */
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final DomainEventJpaRepository events;
    private final List<DomainEventHandler> handlers;

    @Transactional
    public void dispatch(UUID eventId) {
        DomainEventEntity e = events.findById(eventId).orElse(null);
        if (e == null || e.getPublishedAt() != null) {
            return;
        }
        DomainEventRecord record = new DomainEventRecord(e.getId(), e.getProjectId(), e.getSeq(),
                e.getAggregateType(), e.getAggregateId(), e.getEventType(), e.getPayload(),
                e.getCorrelationId(), e.getOccurredAt());
        for (DomainEventHandler handler : handlers) {
            handler.handle(record);
        }
        e.setPublishedAt(Instant.now());
        events.save(e);
    }
}
