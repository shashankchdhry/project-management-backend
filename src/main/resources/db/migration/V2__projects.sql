-- Projects and project membership (RBAC + row-level scoping).

CREATE TABLE projects (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id uuid NOT NULL REFERENCES workspaces(id),
    key          text NOT NULL,                      -- "PROJ"; used in issue keys PROJ-123
    name         text NOT NULL,
    lead_id      uuid REFERENCES users(id),
    workflow_id  uuid NOT NULL REFERENCES workflows(id),
    issue_seq    bigint NOT NULL DEFAULT 0,          -- per-project issue number counter
    event_seq    bigint NOT NULL DEFAULT 0,          -- per-project domain-event sequence (outbox/replay)
    archived_at  timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, key)
);

-- A user can see/act on a project only if a membership row exists.
CREATE TABLE project_memberships (
    project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    uuid NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    role       text NOT NULL CHECK (role IN ('ADMIN', 'PROJECT_LEAD', 'MEMBER', 'VIEWER')),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_membership_user ON project_memberships (user_id);  -- "projects I belong to"
