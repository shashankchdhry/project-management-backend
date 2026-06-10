package org.example.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.WorkflowStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStatusJpaRepository extends JpaRepository<WorkflowStatusEntity, UUID> {

    List<WorkflowStatusEntity> findByWorkflowId(UUID workflowId);
}
