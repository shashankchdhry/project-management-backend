package org.example.adapter.in.web.dto;

import java.util.Set;
import java.util.UUID;

public record CompleteSprintRequest(Set<UUID> carryOverIssueIds, UUID targetSprintId) {
}
