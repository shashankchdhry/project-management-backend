# ADR-0008: PostgreSQL advisory locks for sprint start/complete

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
Completing a sprint is a **multi-step, multi-row** operation — find incomplete issues,
carry selected ones over to the next sprint, recompute velocity, write audit entries, flip sprint
state. If two "complete sprint" requests (or a complete racing a start) run concurrently, carry-over
could double-apply or velocity could be corrupted. Optimistic locking on a single row
([ADR-0007](0007-optimistic-locking.md)) does not protect an invariant that spans many rows.

## Decision
Guard sprint start/complete with a **transaction-scoped PostgreSQL advisory lock**
(`pg_advisory_xact_lock(key)`), where `key` is derived from the sprint id. The lock serializes the
entire critical section per sprint and is released automatically at transaction end (commit or
rollback), so a crash can't leak it.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Advisory xact lock (chosen)** | Guards the whole multi-row operation; app-defined granularity (per sprint); auto-released at tx end; cheap | Must consistently derive the same key; lock is advisory (only honored by code that takes it) |
| `SELECT ... FOR UPDATE` on the sprint row | Familiar, row-scoped | Locks only the sprint row, not the many issue rows the operation mutates; intent less explicit |
| Optimistic version on sprint only | No locks | Doesn't protect cross-row invariants (carry-over set, velocity) — wrong tool here |
| Distributed lock in Redis | Works across DBs | Adds a failure mode (lock vs DB consistency); unnecessary when the work is all in Postgres |

## Consequences
- **Positive:** sprint transitions are serialized per sprint with a clear, intentional critical
  section; keeps sprint completion correct under concurrency without broad table locking.
- **Trade-off (accepted):** advisory locks are a convention — every code path performing the
  operation must take the lock. We centralize this in a single application service so it can't be
  bypassed, and document the key-derivation scheme in the [concurrency LLD](../lld/concurrency.md).