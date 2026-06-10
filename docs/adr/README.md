# Architecture Decision Records

> **Implementation status.** Realized in code: 0001, 0002, 0003, 0005, 0006, 0007, 0008, 0010,
> 0011, 0012, 0013, and 0014 (custom-field *storage*). Partial / not yet implemented: **0004** (Redis
> pub/sub + rate-limiting are built; the board cache is not) and **0009** (idempotency keys — the
> table exists, no handler yet).

Each ADR captures **one** significant decision: the context that forced it, the option we chose,
the **alternatives we rejected**, and the trade-offs we accepted. They are the answer to "why did
you build it this way and not another way."

Format (lightweight [MADR](https://adr.github.io/madr/)-style): *Context → Decision → Options
considered → Consequences (incl. trade-offs) → Revisit-if*.

| # | Decision | Status |
|---|----------|--------|
| [0001](0001-hexagonal-architecture.md) | Hexagonal (ports & adapters) architecture | Accepted |
| [0002](0002-spring-boot-3.5-over-4.0.md) | Spring Boot 3.5.x over 4.0.x | Accepted |
| [0003](0003-postgresql-primary-store.md) | PostgreSQL as the single system of record | Accepted |
| [0004](0004-redis-roles.md) | Redis for cache, rate limiting, presence, pub/sub | Accepted |
| [0005](0005-cqrs-read-model.md) | CQRS with a denormalized board read model | Accepted |
| [0006](0006-domain-events-outbox.md) | Domain events via a transactional outbox | Accepted |
| [0007](0007-optimistic-locking.md) | Optimistic locking for issue updates | Accepted |
| [0008](0008-advisory-locks-sprint-ops.md) | Advisory locks for sprint start/complete | Accepted |
| [0009](0009-idempotency-keys.md) | Idempotency keys for mutations | Accepted |
| [0010](0010-websocket-stomp-realtime.md) | WebSocket/STOMP + Redis relay + event replay | Accepted |
| [0011](0011-postgres-full-text-search.md) | PostgreSQL full-text search over Elasticsearch | Accepted |
| [0012](0012-api-versioning.md) | URI-prefix API versioning (`/api/v1`) | Accepted |
| [0013](0013-issue-type-modeling.md) | Single-table issue-type modeling | Accepted |
| [0014](0014-custom-fields-jsonb.md) | JSONB-backed custom fields | Accepted |

> Status values: **Accepted** (in force), **Superseded by ADR-XXXX**, **Deprecated**. New
> decisions append; existing ADRs are never rewritten, only superseded — the history is the point.