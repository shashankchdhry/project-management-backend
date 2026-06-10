package org.example.domain.workflow;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.domain.issue.Issue;
import org.example.domain.shared.BusinessRuleException;
import org.example.domain.shared.WorkflowTransitionException;

/**
 * Stateless domain service that applies a workflow to an issue: resolves the transition, evaluates
 * guards, and applies the status change. Guards that need data from other rows (WIP limits, open
 * children) are enforced in the application layer.
 */
public final class WorkflowEngine {

    /**
     * Moves {@code issue} to {@code targetStatusId}.
     *
     * @return the transition that was applied (so the caller can run post-actions)
     * @throws WorkflowTransitionException if no transition exists from the current status
     * @throws BusinessRuleException       if a guard fails
     */
    public WorkflowTransition transition(Issue issue, Workflow workflow, UUID targetStatusId) {
        WorkflowStatus current = workflow.statusById(issue.statusId());
        WorkflowStatus target = workflow.statusById(targetStatusId);

        WorkflowTransition transition = workflow.resolve(issue.statusId(), targetStatusId)
                .orElseThrow(() -> new WorkflowTransitionException(
                        "Cannot transition " + issue.key() + " from '" + current.name()
                                + "' to '" + target.name() + "'",
                        workflow.allowedTargetNamesFrom(issue.statusId())));

        evaluateGuards(transition, issue);
        issue.applyTransition(current.id(), current.name(), target.id(), target.name());
        return transition;
    }

    private void evaluateGuards(WorkflowTransition transition, Issue issue) {
        Map<String, Object> guard = transition.guard();
        if (guard == null || guard.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(guard.get("requireAssignee")) && issue.assigneeId() == null) {
            throw new BusinessRuleException(
                    "Transition '" + transition.name() + "' requires the issue to have an assignee");
        }
        if (guard.get("requireFields") instanceof List<?> fields) {
            for (Object field : fields) {
                if ("story_points".equals(field) && issue.storyPoints() == null) {
                    throw new BusinessRuleException(
                            "Transition '" + transition.name() + "' requires field 'story_points'");
                }
            }
        }
    }
}
