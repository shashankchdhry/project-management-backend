package org.example.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.IssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueJpaRepository extends JpaRepository<IssueEntity, UUID> {

    Optional<IssueEntity> findByKey(String key);

    List<IssueEntity> findBySprintId(UUID sprintId);

    long countByStatusId(UUID statusId);
}
