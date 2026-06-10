package org.example.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {

    Optional<ProjectEntity> findFirstByKey(String key);
}
