package org.example.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.entity.IssueEntity;
import org.example.adapter.out.persistence.mapper.IssueMapper;
import org.example.adapter.out.persistence.repository.IssueJpaRepository;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.domain.issue.Issue;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssuePersistenceAdapter implements IssueRepositoryPort {

    private final IssueJpaRepository repository;
    private final IssueMapper mapper;

    @Override
    public Optional<Issue> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Issue> findByKey(String key) {
        return repository.findByKey(key).map(mapper::toDomain);
    }

    @Override
    public List<Issue> findBySprint(UUID sprintId) {
        return repository.findBySprintId(sprintId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByStatus(UUID statusId) {
        return repository.countByStatusId(statusId);
    }

    @Override
    public Issue save(Issue issue) {
        IssueEntity entity = repository.findById(issue.id())
                .map(existing -> {
                    mapper.applyMutable(issue, existing);
                    return existing;
                })
                .orElseGet(() -> mapper.newEntity(issue));
        // saveAndFlush so the JPA @Version check (and any conflict) happens here, and the
        // returned entity carries the incremented version for the response ETag.
        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}
