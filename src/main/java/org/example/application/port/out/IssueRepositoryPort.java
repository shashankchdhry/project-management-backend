package org.example.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.domain.issue.Issue;

/** Outbound port for loading and persisting {@link Issue} aggregates. */
public interface IssueRepositoryPort {

    Optional<Issue> findById(UUID id);

    Optional<Issue> findByKey(String key);

    List<Issue> findBySprint(UUID sprintId);

    /** Number of issues currently in a status — used for WIP-limit enforcement. */
    long countByStatus(UUID statusId);

    Issue save(Issue issue);
}
