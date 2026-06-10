-- CQRS read model: denormalized, board-optimized projection of issues (docs/adr/0005).
-- Maintained by event projectors; the board query reads this single table.

CREATE TABLE issue_board_view (
    issue_id        uuid PRIMARY KEY REFERENCES issues(id) ON DELETE CASCADE,
    project_id      uuid NOT NULL,
    status_id       uuid NOT NULL,
    status_name     text NOT NULL,
    status_category text NOT NULL,
    rank            text NOT NULL,             -- lexorank-style ordering within a column
    issue_key       text NOT NULL,
    type            text NOT NULL,
    title           text NOT NULL,
    priority        text NOT NULL,
    assignee_id     uuid,
    assignee_name   text,
    story_points    int,
    sprint_id       uuid,
    version         bigint NOT NULL,
    updated_at      timestamptz NOT NULL
);

CREATE INDEX idx_board_view ON issue_board_view (project_id, status_id, rank);
