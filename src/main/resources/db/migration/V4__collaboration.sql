-- Comments (threaded), custom field definitions, notifications.

CREATE TABLE comments (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id          uuid NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    parent_comment_id uuid REFERENCES comments(id),
    author_id         uuid NOT NULL REFERENCES users(id),
    body              text NOT NULL,
    mentions          uuid[] NOT NULL DEFAULT '{}',
    edited_at         timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    search_vector     tsvector GENERATED ALWAYS AS (to_tsvector('english', coalesce(body, ''))) STORED
);

CREATE INDEX idx_comments_issue  ON comments (issue_id, created_at);
CREATE INDEX idx_comments_search ON comments USING GIN (search_vector);

CREATE TABLE custom_field_defs (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    field_key  text NOT NULL,
    name       text NOT NULL,
    type       text NOT NULL CHECK (type IN ('TEXT', 'NUMBER', 'DROPDOWN', 'DATE')),
    options    jsonb,
    required   boolean NOT NULL DEFAULT false,
    UNIQUE (project_id, field_key)
);

-- status supports the circuit-breaker fallback queue: PENDING notifications are drained on recovery.
CREATE TABLE notifications (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         text NOT NULL,
    issue_id     uuid REFERENCES issues(id) ON DELETE CASCADE,
    payload      jsonb NOT NULL,
    status       text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'READ')),
    read_at      timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, read_at, created_at DESC);
CREATE INDEX idx_notifications_pending   ON notifications (status) WHERE status = 'PENDING';
