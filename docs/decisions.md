# Design Decisions

Why the system is built the way it is — the choices that shaped it, and the alternatives they beat.
Grouped by area; each entry is the decision, the reason, and what it was chosen over.

## Architecture & code structure

**Hexagonal (ports & adapters).** The domain — aggregates, domain events, policies — depends on
nothing outward and carries no framework or persistence annotations. The application layer owns the
use cases and transaction boundaries through ports (interfaces); REST and WebSocket are inbound
adapters, JPA/Redis/notifications outbound. The JPA `@Entity` classes live in the persistence
adapter and are mapped to and from the domain aggregates.
*Why:* rich rules (workflow transitions, sprint carry-over, WIP limits) stay unit-testable in
milliseconds with no database or Spring context, and infrastructure is swappable behind ports.
*Instead of:* layered MVC, where the domain leaks JPA/Spring and entities double as both model and
table — the rules become hard to isolate and test.

**CQRS with a board read model.** Writes go through rich, locked, validated aggregates; the board —
the hottest read — is served from a denormalized `issue_board_view` projection kept current by event
projectors. Same database, separate models; not event sourcing, not separate services.
*Why:* the board would otherwise join issues → users → statuses → sprints → custom fields on every
load; a flat, indexed projection serves it cheaply for many concurrent viewers.
*Instead of:* query-time joins plus a cache (doesn't fix the cold read or invalidation) or full event
sourcing (far more complexity than the problem needs). The projection carries a `version`, and the
write path returns authoritative state, so read-your-write holds despite eventual-consistency lag.

**Domain events via a transactional outbox.** Every mutation writes its events to `domain_event_log`
in the *same transaction* as the state change; a relay worker then publishes them to three consumers —
the activity/audit projector, the notification dispatcher (behind a circuit breaker), and the Redis
WebSocket relay. Events carry a per-project monotonic `seq`.
*Why:* side effects must be decoupled so a notification outage can't fail a board write, and there
must never be a state change without its event (or vice-versa). The ordered log doubles as the audit
trail and the missed-event replay stream.
*Instead of:* synchronous in-request side effects (couples the write to notification availability, no
replay) or a message broker as source of truth (reintroduces the dual-write problem without an outbox).

## Data & storage

**PostgreSQL as the single system of record.** One store, leaning on native features:
`pg_advisory_xact_lock`, `tsvector`/GIN full-text search, JSONB, and (optionally) row-level security.
*Why:* the data is highly relational, and one engine covers locking, search, JSON, integrity, and a
state-change-plus-outbox-event in a single atomic transaction.
*Instead of:* MySQL (weaker full-text and advisory-lock/RLS story) or a document store (no
cross-document transactions over the relationship graph). Scale-out path: shard by `workspace_id`.

**Full-text search in Postgres (not Elasticsearch).** Stored, generated `tsvector` columns on issues
(title weight A, description B) and comments, GIN-indexed; structured filters compile to a SQL `WHERE`
on the same query; results are ranked by `ts_rank` and cursor-paginated.
*Why:* full-text combined with structured filters, indexed and transactionally consistent with the
data, with no extra infrastructure.
*Instead of:* Elasticsearch — a separate system, an ingestion pipeline, and eventual consistency —
unjustified at this scale. It stays the documented scale-out option, fed from the same event stream.

**Single-table issue types.** Epic, Story, Task, Bug, and Sub-task share one `issues` table with a
`type` discriminator and a self-referencing `parent_id`; which type may parent which is enforced in
the domain, not the schema.
*Why:* the types share most attributes and are queried together constantly (board, search, feed); one
flat table with one index set keeps those dominant queries fast and polymorphic.
*Instead of:* joined inheritance (every read and write joins) or table-per-type (no simple query
across types — exactly what the board and search need).

**JSONB-backed custom fields.** Per-project field definitions live in `custom_field_defs`; the values
live in an `issues.custom_fields jsonb` column (GIN-indexable), validated against the definitions on
write.
*Why:* custom-field sets are defined per project at runtime and vary, so they can't be fixed columns;
JSONB keeps the issue a single, cheap-to-read row.
*Instead of:* EAV rows (turn every issue read into a join + pivot) or a wide spare-column table (a
rigid field cap, wasteful, can't model dropdown options).

## Concurrency & integrity

**Optimistic locking for issue updates.** A `version` column; updates run
`UPDATE … SET version = version + 1 WHERE id = ? AND version = ?`; zero rows affected → 409 Conflict
carrying the current state for the client to merge and retry.
*Why:* edits are low-contention, and holding a row lock across a user's think-time causes contention
and deadlocks.
*Instead of:* pessimistic `SELECT … FOR UPDATE` (lock waits, deadlocks) or last-write-wins (silent
data loss). Multi-row operations use advisory locks instead.

**Advisory locks for sprint start/complete.** A transaction-scoped `pg_advisory_xact_lock` keyed by
sprint id serializes the whole critical section (find incomplete issues, carry over, recompute
velocity, flip state); the same mechanism keyed by `(project, status)` makes WIP-limit checks
race-safe. The lock auto-releases at transaction end.
*Why:* sprint completion spans many rows, so a single-row `version` can't protect its invariant.
*Instead of:* `SELECT … FOR UPDATE` on the sprint row (doesn't cover the mutated issue rows) or a
Redis lock (adds a lock-vs-DB consistency failure mode when all the work is already in Postgres).

**Idempotency keys.** *(Designed; table migrated, handler not yet implemented.)* Mutations accept an
`Idempotency-Key` header; the first result is stored against the key and replayed on retry, and reuse
with a different request fingerprint is rejected.
*Why:* clients retry on timeouts and network blips, and a retried create or transition must not
double-apply.

## Real-time & infrastructure

**Redis as the cross-instance backplane.** Four roles: board-state cache, token-bucket rate limiting,
presence sets, and the pub/sub relay that fans events out to every instance's WebSocket sessions.
*(Pub/sub and rate limiting are built; the cache and presence are designed.)*
*Why:* a stateless, horizontally scalable app tier needs shared state for cross-instance concerns —
most importantly delivering a WebSocket event to a client connected to a *different* instance.
*Instead of:* in-process structures (per-instance cache, non-global limits, no cross-instance
delivery) or Kafka for fan-out (heavier ops, and you'd still need a cache/limit store).

**WebSocket/STOMP with a Redis relay and event replay.** Clients subscribe to per-project STOMP
topics; the outbox relay publishes each event to Redis pub/sub, and every instance forwards it to its
locally-connected sessions. Each event carries the per-project `seq`, so a reconnecting client replays
everything after its last-seen sequence (`GET …/events?after=`); delivery is at-least-once and clients
dedupe by `seq`.
*Why:* board changes must reach clients across a horizontally scaled tier, and reconnects must recover
missed events.
*Instead of:* raw WebSocket (hand-rolled framing and relay), SSE (one-way, weak presence), sticky
sessions (fragile on rebalance), or an in-memory replay buffer (lost on restart).

## API & platform

**URI-prefix API versioning (`/api/v1`).** The version lives in the path; only additive changes within
a version, a new `/api/v2` for breaking ones. Versioning is confined to the web adapter; the domain
stays version-agnostic.
*Why:* visible, unambiguous, and trivial to route, test, and document.
*Instead of:* header/media-type negotiation (hard to test and browse) or no versioning (guarantees a
painful break later).

**Spring Boot 3.5 over 4.0.** Pinned to the 3.5.x line with springdoc 2.x and Resilience4j's
`spring-boot3` starter.
*Why:* the libraries we depend on are battle-tested on the Boot 3 / Framework 6 line; for a release
where a startup surprise is expensive, proven beats newest.
*Instead of:* Boot 4.0 (Resilience4j/springdoc support unproven there) or a 4.1 release candidate
(not GA).
