package org.example.config.security;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.domain.project.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** RBAC + row-level scoping: checks a user's role on a project. */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final JdbcTemplate jdbc;

    public void requireProjectRole(UUID userId, String projectKey, Role minimum) {
        List<String> roles = jdbc.queryForList("""
                SELECT m.role FROM project_memberships m
                JOIN projects p ON p.id = m.project_id
                WHERE p.key = ? AND m.user_id = ?
                """, String.class, projectKey, userId);
        if (roles.isEmpty()) {
            throw new AccessDeniedException("You are not a member of project " + projectKey);
        }
        Role role = Role.valueOf(roles.get(0));
        if (!role.atLeast(minimum)) {
            throw new AccessDeniedException("Requires " + minimum + " on " + projectKey + " (you are " + role + ")");
        }
    }
}
