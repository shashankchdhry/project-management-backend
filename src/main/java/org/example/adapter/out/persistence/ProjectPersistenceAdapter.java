package org.example.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.entity.ProjectEntity;
import org.example.adapter.out.persistence.repository.ProjectJpaRepository;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.domain.project.Project;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepositoryPort {

    private final ProjectJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public Optional<Project> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Project> findByKey(String key) {
        return repository.findFirstByKey(key).map(this::toDomain);
    }

    @Override
    public long nextIssueSeq(UUID projectId) {
        return allocate("issue_seq", projectId);
    }

    @Override
    public long nextEventSeq(UUID projectId) {
        return allocate("event_seq", projectId);
    }

    // Atomic single-statement increment-and-return; relies on the caller's transaction.
    private long allocate(String column, UUID projectId) {
        Object result = entityManager
                .createNativeQuery("UPDATE projects SET " + column + " = " + column + " + 1 WHERE id = :id RETURNING " + column)
                .setParameter("id", projectId)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private Project toDomain(ProjectEntity e) {
        return new Project(e.getId(), e.getWorkspaceId(), e.getKey(), e.getName(), e.getWorkflowId(), e.getLeadId());
    }
}
