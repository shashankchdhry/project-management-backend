package org.example.application.query;

import java.util.UUID;

/**
 * Search inputs: optional full-text {@code q}, structured filters, and keyset pagination.
 * All filters are optional and combine with AND.
 */
public record IssueSearchCriteria(
        String projectKey,
        String q,
        String status,     // status name, e.g. "In Progress"
        UUID assignee,
        String type,
        String priority,
        UUID sprintId,
        String cursor,
        int limit) {
}
