package org.example.application.port.out;

import java.util.UUID;
import org.example.domain.workflow.Workflow;

public interface WorkflowRepositoryPort {

    /** Assemble the {@link Workflow} aggregate (statuses + transitions) for a workflow id. */
    Workflow load(UUID workflowId);
}
