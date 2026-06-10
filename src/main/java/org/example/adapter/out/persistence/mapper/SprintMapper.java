package org.example.adapter.out.persistence.mapper;

import java.time.Instant;
import org.example.adapter.out.persistence.entity.SprintEntity;
import org.example.domain.sprint.Sprint;
import org.example.domain.sprint.SprintState;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

    public Sprint toDomain(SprintEntity e) {
        return Sprint.rehydrate(
                e.getId(), e.getProjectId(), e.getName(), e.getGoal(), SprintState.valueOf(e.getState()),
                e.getStartDate(), e.getEndDate(), e.getCompletedAt(), e.getVelocity(), e.getVersion());
    }

    public SprintEntity newEntity(Sprint s) {
        SprintEntity e = new SprintEntity();
        e.setId(s.id());
        e.setProjectId(s.projectId());
        e.setCreatedAt(Instant.now());
        applyMutable(s, e);
        return e;
    }

    public void applyMutable(Sprint s, SprintEntity e) {
        e.setName(s.name());
        e.setGoal(s.goal());
        e.setState(s.state().name());
        e.setStartDate(s.startDate());
        e.setEndDate(s.endDate());
        e.setCompletedAt(s.completedAt());
        e.setVelocity(s.velocity());
    }
}
