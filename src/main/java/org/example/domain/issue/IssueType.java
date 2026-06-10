package org.example.domain.issue;

/**
 * Issue types and the parent-child rules between them (Epic → Story/Task/Bug → Sub-task).
 * Hierarchy rules live in the domain, not the schema.
 */
public enum IssueType {
    EPIC,
    STORY,
    TASK,
    BUG,
    SUBTASK;

    /** Whether an issue of this type may be the parent of a {@code child}-typed issue. */
    public boolean canParent(IssueType child) {
        return switch (this) {
            case EPIC -> child == STORY || child == TASK || child == BUG;
            case STORY, TASK, BUG -> child == SUBTASK;
            case SUBTASK -> false;
        };
    }

    public boolean canHaveParent() {
        return this != EPIC;
    }
}
