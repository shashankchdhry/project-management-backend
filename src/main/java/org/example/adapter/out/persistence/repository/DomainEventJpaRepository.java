package org.example.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.example.adapter.out.persistence.entity.DomainEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DomainEventJpaRepository extends JpaRepository<DomainEventEntity, UUID> {

    /** Outbox poll: ids of not-yet-relayed events in occurrence order. */
    @Query("select e.id from DomainEventEntity e where e.publishedAt is null order by e.occurredAt asc, e.seq asc")
    List<UUID> findUnpublishedIds(Pageable pageable);

    /** Replay: events for a project after a given sequence, in order. */
    List<DomainEventEntity> findByProjectIdAndSeqGreaterThanOrderBySeqAsc(UUID projectId, long seq);
}
