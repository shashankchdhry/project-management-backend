package org.example.domain.sprint;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.shared.AggregateRoot;
import org.example.domain.shared.BusinessRuleException;

/**
 * The Sprint aggregate: a simple state machine FUTURE → ACTIVE → CLOSED. Carry-over and velocity
 * computation, which span many issues, live in {@link SprintCompletion} (a domain service).
 */
public class Sprint extends AggregateRoot {

    private final UUID id;
    private final UUID projectId;
    private String name;
    private String goal;
    private SprintState state;
    private Instant startDate;
    private Instant endDate;
    private Instant completedAt;
    private Integer velocity;
    private long version;

    private Sprint(UUID id, UUID projectId, String name, String goal, SprintState state, Instant startDate,
                   Instant endDate, Instant completedAt, Integer velocity, long version) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.goal = goal;
        this.state = state;
        this.startDate = startDate;
        this.endDate = endDate;
        this.completedAt = completedAt;
        this.velocity = velocity;
        this.version = version;
    }

    public static Sprint create(UUID id, UUID projectId, String name, String goal, Instant startDate, Instant endDate) {
        return new Sprint(id, projectId, name, goal, SprintState.FUTURE, startDate, endDate, null, null, 0L);
    }

    public static Sprint rehydrate(UUID id, UUID projectId, String name, String goal, SprintState state,
                                   Instant startDate, Instant endDate, Instant completedAt, Integer velocity,
                                   long version) {
        return new Sprint(id, projectId, name, goal, state, startDate, endDate, completedAt, velocity, version);
    }

    public void start(Instant when) {
        if (state != SprintState.FUTURE) {
            throw new BusinessRuleException("Only a FUTURE sprint can be started (current state: " + state + ")");
        }
        state = SprintState.ACTIVE;
        if (startDate == null) {
            startDate = when;
        }
        registerEvent(new SprintStarted(id, projectId, name, when));
    }

    /** Close the sprint with its computed velocity. Called by {@link SprintCompletion}. */
    public void markCompleted(int finalVelocity, Instant when) {
        if (state != SprintState.ACTIVE) {
            throw new BusinessRuleException("Only an ACTIVE sprint can be completed (current state: " + state + ")");
        }
        state = SprintState.CLOSED;
        completedAt = when;
        velocity = finalVelocity;
        registerEvent(new SprintCompleted(id, projectId, finalVelocity, when));
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public String name() { return name; }
    public String goal() { return goal; }
    public SprintState state() { return state; }
    public Instant startDate() { return startDate; }
    public Instant endDate() { return endDate; }
    public Instant completedAt() { return completedAt; }
    public Integer velocity() { return velocity; }
    public long version() { return version; }
}
