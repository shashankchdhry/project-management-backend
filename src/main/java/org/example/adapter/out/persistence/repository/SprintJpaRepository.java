package org.example.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.SprintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintJpaRepository extends JpaRepository<SprintEntity, UUID> {

    Optional<SprintEntity> findFirstByProjectIdAndState(UUID projectId, String state);

    List<SprintEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
