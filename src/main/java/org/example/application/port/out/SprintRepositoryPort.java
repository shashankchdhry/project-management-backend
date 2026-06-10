package org.example.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.domain.sprint.Sprint;

public interface SprintRepositoryPort {

    Optional<Sprint> findById(UUID id);

    Optional<Sprint> findActiveByProject(UUID projectId);

    List<Sprint> findByProject(UUID projectId);

    Sprint save(Sprint sprint);
}
