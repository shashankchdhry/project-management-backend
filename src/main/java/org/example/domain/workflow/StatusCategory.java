package org.example.domain.workflow;

/**
 * Coarse grouping of a workflow status. Drives board columns and "done" semantics
 * (velocity, sprint carry-over).
 */
public enum StatusCategory {
    TODO,
    IN_PROGRESS,
    DONE
}
