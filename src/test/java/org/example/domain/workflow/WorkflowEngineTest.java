package org.example.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.domain.workflow.TestWorkflowFactory.DONE;
import static org.example.domain.workflow.TestWorkflowFactory.IN_PROGRESS;
import static org.example.domain.workflow.TestWorkflowFactory.TODO;

import java.util.UUID;
import org.example.domain.issue.Issue;
import org.example.domain.issue.IssueDetailsUpdate;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.example.domain.issue.StatusChanged;
import org.example.domain.shared.BusinessRuleException;
import org.example.domain.shared.WorkflowTransitionException;
import org.junit.jupiter.api.Test;

class WorkflowEngineTest {

    private final WorkflowEngine engine = new WorkflowEngine();

    private Issue newIssue(UUID statusId) {
        return Issue.create(UUID.randomUUID(), "PROJ-1", 1, UUID.randomUUID(), IssueType.STORY,
                "Title", "Desc", statusId, Priority.MEDIUM, UUID.randomUUID(), null, null);
    }

    @Test
    void appliesValidTransitionAndRaisesStatusChanged() {
        Issue issue = newIssue(TODO);
        issue.pullDomainEvents(); // discard IssueCreated

        engine.transition(issue, TestWorkflowFactory.standard(), IN_PROGRESS);

        assertThat(issue.statusId()).isEqualTo(IN_PROGRESS);
        assertThat(issue.domainEvents()).anySatisfy(event -> {
            assertThat(event).isInstanceOf(StatusChanged.class);
            StatusChanged changed = (StatusChanged) event;
            assertThat(changed.fromStatus()).isEqualTo("To Do");
            assertThat(changed.toStatus()).isEqualTo("In Progress");
        });
    }

    @Test
    void rejectsIllegalTransitionWithAllowedList() {
        Issue issue = newIssue(TODO);

        assertThatThrownBy(() -> engine.transition(issue, TestWorkflowFactory.standard(), DONE))
                .isInstanceOf(WorkflowTransitionException.class)
                .satisfies(ex -> assertThat(((WorkflowTransitionException) ex).allowedTransitions())
                        .containsExactly("In Progress"));
        assertThat(issue.statusId()).isEqualTo(TODO); // unchanged
    }

    @Test
    void enforcesRequireAssigneeGuard() {
        Issue issue = newIssue(TODO); // no assignee

        assertThatThrownBy(() -> engine.transition(issue, TestWorkflowFactory.withAssigneeGuardOnStart(), IN_PROGRESS))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(issue.statusId()).isEqualTo(TODO);
    }

    @Test
    void passesGuardWhenAssigneePresent() {
        Issue issue = newIssue(TODO);
        issue.updateDetails(new IssueDetailsUpdate(null, null, null, null, UUID.randomUUID(), null));
        issue.pullDomainEvents();

        engine.transition(issue, TestWorkflowFactory.withAssigneeGuardOnStart(), IN_PROGRESS);

        assertThat(issue.statusId()).isEqualTo(IN_PROGRESS);
    }
}
