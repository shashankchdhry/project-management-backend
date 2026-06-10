package org.example.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;

public record CreateIssueRequest(
        @NotNull IssueType type,
        @NotBlank String title,
        String description,
        Priority priority,
        UUID reporterId,
        UUID parentId,
        Integer storyPoints) {
}
