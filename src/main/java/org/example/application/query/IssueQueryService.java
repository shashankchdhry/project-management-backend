package org.example.application.query;

import lombok.RequiredArgsConstructor;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.issue.Issue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueQueryService {

    private final IssueRepositoryPort issues;

    @Transactional(readOnly = true)
    public Issue getByKey(String issueKey) {
        return issues.findByKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue " + issueKey + " not found"));
    }
}
