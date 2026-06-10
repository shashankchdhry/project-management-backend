package org.example.application.command;

import java.util.UUID;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;

public record CreateIssueCommand(
        String projectKey,
        IssueType type,
        String title,
        String description,
        Priority priority,
        UUID reporterId,
        UUID parentId,
        Integer storyPoints) {
}
