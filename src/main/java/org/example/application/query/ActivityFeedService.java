package org.example.application.query;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.ActivityFeedPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.config.security.CurrentUser;
import org.example.config.security.PermissionService;
import org.example.domain.project.Project;
import org.example.domain.project.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityFeedService {

    private final ProjectRepositoryPort projects;
    private final ActivityFeedPort feedPort;
    private final PermissionService permissions;

    @Transactional(readOnly = true)
    public ActivityPage feed(String projectKey, String eventType, UUID issueId, String cursor, int limit) {
        Project project = projects.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectKey + " not found"));
        // Row-level scoping: only project members may read the feed (skipped when unauthenticated).
        CurrentUser.id().ifPresent(userId -> permissions.requireProjectRole(userId, projectKey, Role.VIEWER));
        return feedPort.feed(project.id(), eventType, issueId, cursor, limit);
    }
}
