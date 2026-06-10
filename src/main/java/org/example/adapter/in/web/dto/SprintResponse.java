package org.example.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;
import org.example.domain.sprint.Sprint;

public record SprintResponse(
        UUID id,
        UUID projectId,
        String name,
        String goal,
        String state,
        Instant startDate,
        Instant endDate,
        Instant completedAt,
        Integer velocity,
        long version) {

    public static SprintResponse from(Sprint s) {
        return new SprintResponse(s.id(), s.projectId(), s.name(), s.goal(), s.state().name(),
                s.startDate(), s.endDate(), s.completedAt(), s.velocity(), s.version());
    }
}
