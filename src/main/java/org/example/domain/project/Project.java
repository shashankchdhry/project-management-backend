package org.example.domain.project;

import java.util.UUID;

/**
 * Lightweight project reference used by application handlers (workflow lookup, issue-key prefix).
 * Projects are largely configuration; the rich behaviour lives on issues/sprints.
 */
public record Project(UUID id, UUID workspaceId, String key, String name, UUID workflowId, UUID leadId) {
}
