package org.example.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.entity.SprintEntity;
import org.example.adapter.out.persistence.mapper.SprintMapper;
import org.example.adapter.out.persistence.repository.SprintJpaRepository;
import org.example.application.port.out.SprintRepositoryPort;
import org.example.domain.sprint.Sprint;
import org.example.domain.sprint.SprintState;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintPersistenceAdapter implements SprintRepositoryPort {

    private final SprintJpaRepository repository;
    private final SprintMapper mapper;

    @Override
    public Optional<Sprint> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Sprint> findActiveByProject(UUID projectId) {
        return repository.findFirstByProjectIdAndState(projectId, SprintState.ACTIVE.name()).map(mapper::toDomain);
    }

    @Override
    public List<Sprint> findByProject(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Sprint save(Sprint sprint) {
        SprintEntity entity = repository.findById(sprint.id())
                .map(existing -> {
                    mapper.applyMutable(sprint, existing);
                    return existing;
                })
                .orElseGet(() -> mapper.newEntity(sprint));
        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}
