package org.example.adapter.out.persistence;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.mapper.WorkflowMapper;
import org.example.adapter.out.persistence.repository.WorkflowStatusJpaRepository;
import org.example.adapter.out.persistence.repository.WorkflowTransitionJpaRepository;
import org.example.application.port.out.WorkflowRepositoryPort;
import org.example.domain.workflow.Workflow;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowPersistenceAdapter implements WorkflowRepositoryPort {

    private final WorkflowStatusJpaRepository statuses;
    private final WorkflowTransitionJpaRepository transitions;
    private final WorkflowMapper mapper;

    @Override
    public Workflow load(UUID workflowId) {
        return mapper.toDomain(
                workflowId,
                statuses.findByWorkflowId(workflowId),
                transitions.findByWorkflowId(workflowId));
    }
}
