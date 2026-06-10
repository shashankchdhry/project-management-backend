-- Audit/activity trail, transactional outbox + event stream, idempotency keys.

-- Append-only audit trail + activity-feed source. Never deleted.
CREATE TABLE activity_log (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    issue_id       uuid REFERENCES issues(id) ON DELETE SET NULL,
    actor_id       uuid REFERENCES users(id),
    event_type     text NOT NULL,
    summary        text NOT NULL,
    changes        jsonb,
    correlation_id text,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_feed  ON activity_log (project_id, created_at DESC, id);
CREATE INDEX idx_activity_issue ON activity_log (issue_id, created_at);

-- Transactional outbox + ordered, replayable event stream.
-- seq is the per-project monotonic counter (projects.event_seq); published_at NULL = not yet relayed.
CREATE TABLE domain_event_log (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    seq            bigint NOT NULL,
    aggregate_type text NOT NULL,
    aggregate_id   uuid NOT NULL,
    event_type     text NOT NULL,
    payload        jsonb NOT NULL,
    correlation_id text,
    occurred_at    timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz,
    UNIQUE (project_id, seq)
);

CREATE INDEX idx_event_outbox    ON domain_event_log (project_id, seq) WHERE published_at IS NULL;
CREATE INDEX idx_event_aggregate ON domain_event_log (aggregate_id, seq);

-- Safe retries for mutations; stores the first response to replay on duplicate keys.
CREATE TABLE idempotency_keys (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     text NOT NULL,
    user_id             uuid NOT NULL REFERENCES users(id),
    request_fingerprint text NOT NULL,
    response_status     int,
    response_body       jsonb,
    created_at          timestamptz NOT NULL DEFAULT now(),
    expires_at          timestamptz NOT NULL,
    UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_idempotency_expiry ON idempotency_keys (expires_at);
