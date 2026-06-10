package org.example.domain.sprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.example.domain.issue.Issue;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.example.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

class SprintCompletionTest {

    private final SprintCompletion completion = new SprintCompletion();
    private final UUID projectId = UUID.randomUUID();
    private final UUID doneStatus = UUID.randomUUID();
    private final UUID todoStatus = UUID.randomUUID();

    private Issue issue(UUID statusId, Integer points) {
        return Issue.create(UUID.randomUUID(), "PROJ-x", 1, projectId, IssueType.STORY,
                "t", "d", statusId, Priority.MEDIUM, UUID.randomUUID(), null, points);
    }

    private Sprint activeSprint() {
        Sprint sprint = Sprint.create(UUID.randomUUID(), projectId, "Sprint 1", "goal", null, null);
        sprint.start(Instant.now());
        sprint.pullDomainEvents();
        return sprint;
    }

    @Test
    void computesVelocityCarriesOverSelectedAndCloses() {
        Sprint sprint = activeSprint();
        Issue done1 = issue(doneStatus, 3);
        Issue done2 = issue(doneStatus, 5);
        Issue open1 = issue(todoStatus, 2);
        Issue open2 = issue(todoStatus, 8);
        Issue open3 = issue(todoStatus, 1);
        List<Issue> issues = List.of(done1, done2, open1, open2, open3);
        UUID nextSprint = UUID.randomUUID();

        SprintCompletionResult result = completion.complete(
                sprint, issues, Set.of(doneStatus),
                Set.of(open1.id(), open2.id()), nextSprint, Instant.now());

        assertThat(result.velocity()).isEqualTo(8); // 3 + 5
        assertThat(result.incompleteIssueIds()).hasSize(3);
        assertThat(result.carriedOverIssueIds()).containsExactlyInAnyOrder(open1.id(), open2.id());
        assertThat(open1.sprintId()).isEqualTo(nextSprint);
        assertThat(open3.sprintId()).isNull(); // not carried over
        assertThat(sprint.state()).isEqualTo(SprintState.CLOSED);
        assertThat(sprint.velocity()).isEqualTo(8);
    }

    @Test
    void cannotCompleteNonActiveSprint() {
        Sprint future = Sprint.create(UUID.randomUUID(), projectId, "Sprint 2", null, null, null);

        assertThatThrownBy(() -> completion.complete(
                future, List.of(), Set.of(doneStatus), Set.of(), null, Instant.now()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
