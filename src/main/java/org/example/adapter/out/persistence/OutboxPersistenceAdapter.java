package org.example.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.entity.DomainEventEntity;
import org.example.adapter.out.persistence.repository.DomainEventJpaRepository;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.domain.shared.DomainEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements EventOutboxPort {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final DomainEventJpaRepository repository;
    private final ProjectRepositoryPort projects;
    private final ObjectMapper objectMapper;

    @Override
    public void append(UUID projectId, List<DomainEvent> events, String correlationId) {
        for (DomainEvent event : events) {
            DomainEventEntity row = new DomainEventEntity();
            row.setId(UUID.randomUUID());
            row.setProjectId(projectId);
            row.setSeq(projects.nextEventSeq(projectId));
            row.setAggregateType(aggregateType(event));
            row.setAggregateId(event.aggregateId());
            row.setEventType(event.eventType());
            row.setPayload(objectMapper.convertValue(event, PAYLOAD_TYPE));
            row.setCorrelationId(correlationId);
            row.setOccurredAt(event.occurredAt());
            repository.save(row);
        }
    }

    private static String aggregateType(DomainEvent event) {
        String type = event.eventType();
        if (type.startsWith("Issue")) {
            return "ISSUE";
        }
        if (type.startsWith("Sprint")) {
            return "SPRINT";
        }
        if (type.startsWith("Comment")) {
            return "COMMENT";
        }
        return "UNKNOWN";
    }
}
