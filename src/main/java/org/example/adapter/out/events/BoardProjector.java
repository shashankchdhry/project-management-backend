package org.example.adapter.out.events;

import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.BoardViewAdapter;
import org.example.adapter.out.persistence.repository.IssueJpaRepository;
import org.example.adapter.out.persistence.repository.WorkflowStatusJpaRepository;
import org.example.application.event.DomainEventHandler;
import org.example.application.event.DomainEventRecord;
import org.springframework.stereotype.Component;

/** Keeps the board read model (issue_board_view) in sync by rebuilding an issue's card on each event. */
@Component
@RequiredArgsConstructor
public class BoardProjector implements DomainEventHandler {

    private final IssueJpaRepository issues;
    private final WorkflowStatusJpaRepository statuses;
    private final BoardViewAdapter boardView;

    @Override
    public void handle(DomainEventRecord event) {
        if (!"ISSUE".equals(event.aggregateType())) {
            return;
        }
        issues.findById(event.aggregateId()).ifPresent(issue ->
                statuses.findById(issue.getStatusId()).ifPresent(status ->
                        boardView.upsert(issue, status)));
    }
}
