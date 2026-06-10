package org.example.domain.workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A configurable status workflow: its statuses (columns) and the transitions allowed between them.
 * The workflow is data ({@code workflow_*} tables), so each project can define its own.
 */
public final class Workflow {

    private final UUID id;
    private final String name;
    private final Map<UUID, WorkflowStatus> statusesById;
    private final List<WorkflowTransition> transitions;

    public Workflow(UUID id, String name, List<WorkflowStatus> statuses, List<WorkflowTransition> transitions) {
        this.id = id;
        this.name = name;
        Map<UUID, WorkflowStatus> byId = new LinkedHashMap<>();
        for (WorkflowStatus status : statuses) {
            byId.put(status.id(), status);
        }
        this.statusesById = Map.copyOf(byId);
        this.transitions = List.copyOf(transitions);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public WorkflowStatus statusById(UUID statusId) {
        WorkflowStatus status = statusesById.get(statusId);
        if (status == null) {
            throw new IllegalArgumentException("Unknown workflow status: " + statusId);
        }
        return status;
    }

    public List<WorkflowStatus> statuses() {
        return List.copyOf(statusesById.values());
    }

    /** The status new issues enter — the target of the initial (from-less) transition. */
    public WorkflowStatus initialStatus() {
        return transitions.stream()
                .filter(WorkflowTransition::isInitial)
                .findFirst()
                .map(t -> statusById(t.toStatusId()))
                .orElseThrow(() -> new IllegalStateException("Workflow '" + name + "' has no initial transition"));
    }

    /** The transition from {@code fromStatusId} to {@code toStatusId}, if one is defined. */
    public Optional<WorkflowTransition> resolve(UUID fromStatusId, UUID toStatusId) {
        return transitions.stream()
                .filter(t -> !t.isInitial())
                .filter(t -> Objects.equals(t.fromStatusId(), fromStatusId) && t.toStatusId().equals(toStatusId))
                .findFirst();
    }

    /** Statuses reachable from {@code fromStatusId} (for 422 "allowed transitions"). */
    public List<WorkflowStatus> allowedTargetsFrom(UUID fromStatusId) {
        return transitions.stream()
                .filter(t -> !t.isInitial())
                .filter(t -> Objects.equals(t.fromStatusId(), fromStatusId))
                .map(t -> statusById(t.toStatusId()))
                .toList();
    }

    public List<String> allowedTargetNamesFrom(UUID fromStatusId) {
        return allowedTargetsFrom(fromStatusId).stream().map(WorkflowStatus::name).toList();
    }
}
