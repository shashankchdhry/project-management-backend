# ADR-0003: PostgreSQL as the single system of record

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
The data is highly relational (projects → issues → sub-tasks, sprints, memberships, comments) and
several needs map directly onto specific database capabilities: advisory locks for sprint
operations, full-text search, row-level security, flexible custom fields, and a strong transactional
audit trail.

## Decision
Use **PostgreSQL** as the one system of record. Lean on native features rather than bolting on
extra systems: `pg_advisory_xact_lock`, `tsvector`/GIN full-text search, JSONB for custom fields,
and (optionally) RLS policies.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **PostgreSQL (chosen)** | One engine satisfies locking + FTS + JSONB + relational integrity + RLS; mature; great Spring/JPA support | Single-writer scaling needs replicas/sharding eventually |
| MySQL | Popular, fast | Weaker full-text; no advisory locks of the same ergonomics; no RLS as rich; JSON less capable than JSONB |
| MongoDB / document store | Flexible schema, easy custom fields | No cross-document transactions over our relationship graph; we'd reinvent relational integrity and joins |

## Consequences
- **Positive:** fewer moving parts — multiple needs collapse into one well-understood store;
  everything participates in one transaction (e.g. state change + outbox event are atomic).
- **Trade-off (accepted):** a single primary is the eventual write bottleneck. Mitigated by the
  read model + cache (reads), read replicas, and a documented shard-by-`workspace_id` path (HLD §7).
- **Revisit if:** search relevance/scale outgrows PG FTS (see [ADR-0011](0011-postgres-full-text-search.md))
  or write volume exceeds a tuned primary.