package org.example.adapter.out.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.WorkflowStatusEntity;
import org.example.adapter.out.persistence.entity.WorkflowTransitionEntity;
import org.example.domain.workflow.StatusCategory;
import org.example.domain.workflow.Workflow;
import org.example.domain.workflow.WorkflowStatus;
import org.example.domain.workflow.WorkflowTransition;
import org.springframework.stereotype.Component;

@Component
public class WorkflowMapper {

    public Workflow toDomain(UUID workflowId, List<WorkflowStatusEntity> statuses,
                             List<WorkflowTransitionEntity> transitions) {
        List<WorkflowStatus> domainStatuses = statuses.stream()
                .map(s -> new WorkflowStatus(s.getId(), s.getName(), StatusCategory.valueOf(s.getCategory()),
                        s.getPosition(), s.getWipLimit()))
                .toList();
        List<WorkflowTransition> domainTransitions = transitions.stream()
                .map(t -> new WorkflowTransition(t.getId(), t.getName(), t.getFromStatusId(), t.getToStatusId(),
                        t.getGuard(), t.getPostAction()))
                .toList();
        return new Workflow(workflowId, "workflow-" + workflowId, domainStatuses, domainTransitions);
    }
}
