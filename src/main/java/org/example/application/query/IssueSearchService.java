package org.example.application.query;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.IssueSearchPort;
import org.example.config.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueSearchService {

    private final IssueSearchPort searchPort;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public SearchPage search(IssueSearchCriteria criteria) {
        // Row-level scoping: a user only searches projects they belong to. When unauthenticated
        // (security disabled in tests), scope is null and no project filter is applied.
        List<UUID> scope = CurrentUser.id()
                .map(userId -> jdbc.queryForList(
                        "SELECT project_id FROM project_memberships WHERE user_id = ?", UUID.class, userId))
                .orElse(null);
        return searchPort.search(criteria, scope);
    }
}
