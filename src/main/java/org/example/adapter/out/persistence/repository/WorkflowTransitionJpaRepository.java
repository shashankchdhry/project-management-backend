package org.example.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.WorkflowTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionJpaRepository extends JpaRepository<WorkflowTransitionEntity, UUID> {

    List<WorkflowTransitionEntity> findByWorkflowId(UUID workflowId);
}
