package org.example.application.command;

import lombok.RequiredArgsConstructor;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.shared.OptimisticLockConflictException;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.issue.Issue;
import org.example.domain.issue.IssueDetailsUpdate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateIssueService {

    private final IssueRepositoryPort issues;
    private final EventOutboxPort outbox;

    /**
     * Apply a partial update. The {@code expectedVersion} (from the client's If-Match) is checked
     * against the current version — a mismatch is a 409 conflict with the current state.
     */
    @Transactional
    public Issue update(String issueKey, long expectedVersion, IssueDetailsUpdate update, String correlationId) {
        Issue issue = issues.findByKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue " + issueKey + " not found"));
        if (issue.version() != expectedVersion) {
            throw new OptimisticLockConflictException(issue, expectedVersion);
        }
        issue.updateDetails(update);
        Issue saved = issues.save(issue);
        outbox.append(issue.projectId(), issue.pullDomainEvents(), correlationId);
        return saved;
    }
}
