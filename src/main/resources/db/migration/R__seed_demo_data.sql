-- Repeatable, idempotent demo seed: a ready-to-explore workspace, users (password = "password"),
-- a configured workflow, a project, memberships, a sprint, and a few issues (with board-view rows
-- so GET /board shows them immediately). Distinct keys (DEMO/WEB) avoid clashing with test data.

-- bcrypt hash below is for the password "password".
INSERT INTO workspaces(id, key, name) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'DEMO', 'Demo Workspace')
ON CONFLICT DO NOTHING;

INSERT INTO users(id, workspace_id, email, display_name, password_hash) VALUES
    ('d0000000-0000-0000-0000-0000000000a1', 'd0000000-0000-0000-0000-000000000001', 'admin@demo.test',  'Demo Admin',  '$2a$10$MOMO.1DoagVuMHi1cSvpA.rK.zwEBKwNtNLh/zd9WqnzQ9sjLS01W'),
    ('d0000000-0000-0000-0000-0000000000a2', 'd0000000-0000-0000-0000-000000000001', 'lead@demo.test',   'Demo Lead',   '$2a$10$MOMO.1DoagVuMHi1cSvpA.rK.zwEBKwNtNLh/zd9WqnzQ9sjLS01W'),
    ('d0000000-0000-0000-0000-0000000000a3', 'd0000000-0000-0000-0000-000000000001', 'member@demo.test', 'Demo Member', '$2a$10$MOMO.1DoagVuMHi1cSvpA.rK.zwEBKwNtNLh/zd9WqnzQ9sjLS01W')
ON CONFLICT DO NOTHING;

INSERT INTO workflows(id, name) VALUES
    ('d0000000-0000-0000-0000-000000000010', 'Default Workflow')
ON CONFLICT DO NOTHING;

INSERT INTO workflow_statuses(id, workflow_id, name, category, position, wip_limit) VALUES
    ('d0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000010', 'To Do',       'TODO',        0, NULL),
    ('d0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000010', 'In Progress', 'IN_PROGRESS', 1, 3),
    ('d0000000-0000-0000-0000-000000000013', 'd0000000-0000-0000-0000-000000000010', 'In Review',   'IN_PROGRESS', 2, NULL),
    ('d0000000-0000-0000-0000-000000000014', 'd0000000-0000-0000-0000-000000000010', 'Done',        'DONE',        3, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO workflow_transitions(id, workflow_id, name, from_status_id, to_status_id, guard) VALUES
    ('d0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000010', 'Create',            NULL,                                   'd0000000-0000-0000-0000-000000000011', NULL),
    ('d0000000-0000-0000-0000-000000000022', 'd0000000-0000-0000-0000-000000000010', 'Start Progress',    'd0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000012', '{"requireAssignee": true}'),
    ('d0000000-0000-0000-0000-000000000023', 'd0000000-0000-0000-0000-000000000010', 'Submit for Review', 'd0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000013', NULL),
    ('d0000000-0000-0000-0000-000000000024', 'd0000000-0000-0000-0000-000000000010', 'Approve',           'd0000000-0000-0000-0000-000000000013', 'd0000000-0000-0000-0000-000000000014', NULL),
    ('d0000000-0000-0000-0000-000000000025', 'd0000000-0000-0000-0000-000000000010', 'Stop Progress',     'd0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000011', NULL)
ON CONFLICT DO NOTHING;

-- issue_seq = 3 so the next API-created issue is WEB-4 (the three seeded issues are WEB-1..3).
INSERT INTO projects(id, workspace_id, key, name, lead_id, workflow_id, issue_seq) VALUES
    ('d0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-000000000001', 'WEB', 'Web App',
     'd0000000-0000-0000-0000-0000000000a2', 'd0000000-0000-0000-0000-000000000010', 3)
ON CONFLICT DO NOTHING;

INSERT INTO project_memberships(project_id, user_id, role) VALUES
    ('d0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-0000000000a1', 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-0000000000a2', 'PROJECT_LEAD'),
    ('d0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-0000000000a3', 'MEMBER')
ON CONFLICT DO NOTHING;

INSERT INTO sprints(id, project_id, name, goal, state) VALUES
    ('d0000000-0000-0000-0000-000000000040', 'd0000000-0000-0000-0000-000000000030', 'Sprint 1', 'Ship the board MVP', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO issues(id, project_id, key, seq, type, title, description, status_id, priority, assignee_id, reporter_id, story_points, sprint_id) VALUES
    ('d0000000-0000-0000-0000-000000000051', 'd0000000-0000-0000-0000-000000000030', 'WEB-1', 1, 'STORY', 'OAuth 2.0 login',  'Implement the OAuth login flow', 'd0000000-0000-0000-0000-000000000012', 'HIGH',   'd0000000-0000-0000-0000-0000000000a3', 'd0000000-0000-0000-0000-0000000000a2', 5, 'd0000000-0000-0000-0000-000000000040'),
    ('d0000000-0000-0000-0000-000000000052', 'd0000000-0000-0000-0000-000000000030', 'WEB-2', 2, 'BUG',   'Board flickers',   'Cards flicker on drag',          'd0000000-0000-0000-0000-000000000011', 'MEDIUM', NULL,                                   'd0000000-0000-0000-0000-0000000000a3', 2, 'd0000000-0000-0000-0000-000000000040'),
    ('d0000000-0000-0000-0000-000000000053', 'd0000000-0000-0000-0000-000000000030', 'WEB-3', 3, 'TASK',  'Write API docs',   'Document the v1 API',            'd0000000-0000-0000-0000-000000000014', 'LOW',    'd0000000-0000-0000-0000-0000000000a2', 'd0000000-0000-0000-0000-0000000000a1', 1, NULL)
ON CONFLICT DO NOTHING;

-- Board read-model rows so the seeded issues appear on GET /board without waiting for a projection.
INSERT INTO issue_board_view(issue_id, project_id, status_id, status_name, status_category, rank, issue_key, type, title, priority, assignee_id, assignee_name, story_points, sprint_id, version, updated_at) VALUES
    ('d0000000-0000-0000-0000-000000000051', 'd0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-000000000012', 'In Progress', 'IN_PROGRESS', '000000000001', 'WEB-1', 'STORY', 'OAuth 2.0 login', 'HIGH',   'd0000000-0000-0000-0000-0000000000a3', 'Demo Member', 5, 'd0000000-0000-0000-0000-000000000040', 0, now()),
    ('d0000000-0000-0000-0000-000000000052', 'd0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-000000000011', 'To Do',       'TODO',        '000000000002', 'WEB-2', 'BUG',   'Board flickers',  'MEDIUM', NULL,                                   NULL,          2, 'd0000000-0000-0000-0000-000000000040', 0, now()),
    ('d0000000-0000-0000-0000-000000000053', 'd0000000-0000-0000-0000-000000000030', 'd0000000-0000-0000-0000-000000000014', 'Done',        'DONE',        '000000000003', 'WEB-3', 'TASK',  'Write API docs',  'LOW',    'd0000000-0000-0000-0000-0000000000a2', 'Demo Lead',   1, NULL,                                   0, now())
ON CONFLICT DO NOTHING;
