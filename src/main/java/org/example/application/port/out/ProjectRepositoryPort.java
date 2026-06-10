package org.example.application.port.out;

import java.util.Optional;
import java.util.UUID;
import org.example.domain.project.Project;

public interface ProjectRepositoryPort {

    Optional<Project> findById(UUID id);

    Optional<Project> findByKey(String key);

    /** Atomically allocate the next issue number for a project ({@code UPDATE ... RETURNING}). */
    long nextIssueSeq(UUID projectId);

    /** Atomically allocate the next per-project domain-event sequence (outbox ordering / replay). */
    long nextEventSeq(UUID projectId);
}
