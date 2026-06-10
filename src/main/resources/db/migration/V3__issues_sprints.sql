-- Sprints, issues (single-table for all types), watchers.

CREATE TABLE sprints (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name         text NOT NULL,
    goal         text,
    state        text NOT NULL DEFAULT 'FUTURE' CHECK (state IN ('FUTURE', 'ACTIVE', 'CLOSED')),
    start_date   timestamptz,
    end_date     timestamptz,
    completed_at timestamptz,
    velocity     int,
    version      bigint NOT NULL DEFAULT 0,
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_sprints_project ON sprints (project_id);

CREATE TABLE issues (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id    uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    key           text NOT NULL,                      -- "PROJ-123"
    seq           bigint NOT NULL,
    type          text NOT NULL CHECK (type IN ('EPIC', 'STORY', 'TASK', 'BUG', 'SUBTASK')),
    title         text NOT NULL,
    description   text,
    status_id     uuid NOT NULL REFERENCES workflow_statuses(id),
    priority      text NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    parent_id     uuid REFERENCES issues(id),
    sprint_id     uuid REFERENCES sprints(id),
    assignee_id   uuid REFERENCES users(id),
    reporter_id   uuid NOT NULL REFERENCES users(id),
    story_points  int,
    labels        text[] NOT NULL DEFAULT '{}',
    custom_fields jsonb  NOT NULL DEFAULT '{}',
    version       bigint NOT NULL DEFAULT 0,           -- optimistic lock
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B')
    ) STORED,
    UNIQUE (project_id, seq),
    UNIQUE (project_id, key)
);

CREATE INDEX idx_issues_board    ON issues (project_id, status_id);
CREATE INDEX idx_issues_sprint   ON issues (project_id, sprint_id);
CREATE INDEX idx_issues_backlog  ON issues (project_id) WHERE sprint_id IS NULL;
CREATE INDEX idx_issues_assignee ON issues (assignee_id);
CREATE INDEX idx_issues_parent   ON issues (parent_id);
CREATE INDEX idx_issues_search   ON issues USING GIN (search_vector);
CREATE INDEX idx_issues_custom   ON issues USING GIN (custom_fields);

CREATE TABLE watchers (
    issue_id   uuid NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    user_id    uuid NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (issue_id, user_id)
);
