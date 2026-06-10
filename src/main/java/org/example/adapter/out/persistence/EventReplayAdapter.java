package org.example.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.repository.DomainEventJpaRepository;
import org.example.application.port.out.EventReplayPort;
import org.example.application.query.ReplayedEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventReplayAdapter implements EventReplayPort {

    private final DomainEventJpaRepository events;

    @Override
    public List<ReplayedEvent> replay(UUID projectId, long afterSeq) {
        return events.findByProjectIdAndSeqGreaterThanOrderBySeqAsc(projectId, afterSeq).stream()
                .map(e -> new ReplayedEvent(e.getSeq(), e.getEventType(), e.getAggregateType(),
                        e.getAggregateId(), e.getOccurredAt(), e.getPayload()))
                .toList();
    }
}
