package org.example.domain.issue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IssueTypeTest {

    @Test
    void enforcesHierarchyRules() {
        assertThat(IssueType.EPIC.canParent(IssueType.STORY)).isTrue();
        assertThat(IssueType.EPIC.canParent(IssueType.SUBTASK)).isFalse();
        assertThat(IssueType.STORY.canParent(IssueType.SUBTASK)).isTrue();
        assertThat(IssueType.TASK.canParent(IssueType.SUBTASK)).isTrue();
        assertThat(IssueType.SUBTASK.canParent(IssueType.TASK)).isFalse();
    }

    @Test
    void knowsWhichTypesCanHaveAParent() {
        assertThat(IssueType.EPIC.canHaveParent()).isFalse();
        assertThat(IssueType.SUBTASK.canHaveParent()).isTrue();
        assertThat(IssueType.STORY.canHaveParent()).isTrue();
    }
}
