-- Tenancy + configurable workflow.
-- Created before projects, because projects reference workflows (FK ordering).

CREATE TABLE workspaces (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    key        text NOT NULL UNIQUE,
    name       text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspaces(id),
    email         text NOT NULL,
    display_name  text NOT NULL,
    password_hash text NOT NULL,
    status        text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, email)
);

CREATE TABLE workflows (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workflow_statuses (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    name        text NOT NULL,
    category    text NOT NULL CHECK (category IN ('TODO', 'IN_PROGRESS', 'DONE')),
    position    int  NOT NULL,
    wip_limit   int,
    UNIQUE (workflow_id, name),
    UNIQUE (workflow_id, position)
);

-- A null from_status_id marks the initial (creation) transition.
CREATE TABLE workflow_transitions (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id    uuid NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    name           text NOT NULL,
    from_status_id uuid REFERENCES workflow_statuses(id),
    to_status_id   uuid NOT NULL REFERENCES workflow_statuses(id),
    guard          jsonb,
    post_action    jsonb,
    UNIQUE (workflow_id, from_status_id, to_status_id)
);
