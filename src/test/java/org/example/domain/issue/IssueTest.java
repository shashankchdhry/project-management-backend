package org.example.domain.issue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.example.domain.shared.DomainEvent;
import org.junit.jupiter.api.Test;

class IssueTest {

    private Issue newIssue() {
        return Issue.create(UUID.randomUUID(), "PROJ-1", 1, UUID.randomUUID(), IssueType.STORY,
                "Original", "Desc", UUID.randomUUID(), Priority.MEDIUM, UUID.randomUUID(), null, 3);
    }

    @Test
    void createRaisesIssueCreated() {
        Issue issue = newIssue();

        assertThat(issue.domainEvents()).hasSize(1);
        assertThat(issue.domainEvents().get(0)).isInstanceOf(IssueCreated.class);
    }

    @Test
    void updateDetailsRecordsChangesAndRaisesEvent() {
        Issue issue = newIssue();
        issue.pullDomainEvents();

        issue.updateDetails(new IssueDetailsUpdate("New title", null, Priority.HIGH, null, null, null));

        List<DomainEvent> events = issue.domainEvents();
        assertThat(events).hasSize(1);
        IssueUpdated updated = (IssueUpdated) events.get(0);
        assertThat(updated.changes()).containsKeys("title", "priority");
        assertThat(issue.title()).isEqualTo("New title");
        assertThat(issue.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void noOpUpdateRaisesNoEvent() {
        Issue issue = newIssue();
        issue.pullDomainEvents();

        issue.updateDetails(new IssueDetailsUpdate("Original", null, Priority.MEDIUM, null, null, null));

        assertThat(issue.domainEvents()).isEmpty();
    }

    @Test
    void moveToSprintRaisesEvent() {
        Issue issue = newIssue();
        issue.pullDomainEvents();
        UUID sprintId = UUID.randomUUID();

        issue.moveToSprint(sprintId);

        assertThat(issue.sprintId()).isEqualTo(sprintId);
        assertThat(issue.domainEvents().get(0)).isInstanceOf(IssueMovedToSprint.class);
    }
}
