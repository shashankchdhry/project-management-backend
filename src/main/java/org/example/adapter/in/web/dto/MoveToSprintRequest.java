package org.example.adapter.in.web.dto;

import java.util.UUID;

/** @param sprintId destination sprint, or {@code null} to move the issue to the backlog */
public record MoveToSprintRequest(UUID sprintId) {
}
