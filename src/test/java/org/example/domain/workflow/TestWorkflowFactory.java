package org.example.domain.workflow;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a known workflow for tests: To Do → In Progress → In Review → Done (plus In Progress → To
 * Do). Deliberately has NO To Do → Done transition, so it exercises that rejection path.
 */
public final class TestWorkflowFactory {

    public static final UUID TODO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final UUID IN_PROGRESS = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    public static final UUID IN_REVIEW = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    public static final UUID DONE = UUID.fromString("00000000-0000-0000-0000-0000000000a4");

    private TestWorkflowFactory() {
    }

    public static Workflow standard() {
        return build(null);
    }

    public static Workflow withAssigneeGuardOnStart() {
        return build(Map.<String, Object>of("requireAssignee", true));
    }

    private static Workflow build(Map<String, Object> startGuard) {
        List<WorkflowStatus> statuses = List.of(
                new WorkflowStatus(TODO, "To Do", StatusCategory.TODO, 0, null),
                new WorkflowStatus(IN_PROGRESS, "In Progress", StatusCategory.IN_PROGRESS, 1, null),
                new WorkflowStatus(IN_REVIEW, "In Review", StatusCategory.IN_PROGRESS, 2, null),
                new WorkflowStatus(DONE, "Done", StatusCategory.DONE, 3, null));
        List<WorkflowTransition> transitions = List.of(
                new WorkflowTransition(UUID.randomUUID(), "Create", null, TODO, null, null),
                new WorkflowTransition(UUID.randomUUID(), "Start Progress", TODO, IN_PROGRESS, startGuard, null),
                new WorkflowTransition(UUID.randomUUID(), "Submit for Review", IN_PROGRESS, IN_REVIEW, null, null),
                new WorkflowTransition(UUID.randomUUID(), "Approve", IN_REVIEW, DONE, null, null),
                new WorkflowTransition(UUID.randomUUID(), "Stop Progress", IN_PROGRESS, TODO, null, null));
        return new Workflow(UUID.randomUUID(), "Default", statuses, transitions);
    }
}
