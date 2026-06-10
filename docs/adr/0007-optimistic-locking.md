# ADR-0007: Optimistic locking for issue updates

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
Two users update the same issue concurrently; the expected behavior is that the second write is
detected as a conflict and gets **409** with the current version so the client can merge and retry.
Issue edits are, in practice, **low-contention** (two people editing the exact same
issue field at the exact same moment is rare).

## Decision
Use **optimistic locking**: a `version bigint` column on `issues` (and `sprints`). Updates run
`UPDATE ... SET ..., version = version + 1 WHERE id = ? AND version = ?`. Zero rows affected means
the version moved underneath us → return **409 Conflict** with the current version and state.
Clients send the expected version via `If-Match`/a `version` field.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Optimistic (chosen)** | No DB lock held across think-time; scales under read-heavy load; gives clean 409-on-conflict semantics | Client must handle conflict + retry |
| Pessimistic (`SELECT ... FOR UPDATE`) | No client-side retry logic | Holds row locks → contention, lock-wait timeouts, deadlock risk; bad fit for interactive editing |
| Last-write-wins (no versioning) | Trivial | Silent data loss; loses concurrent updates |

## Consequences
- **Positive:** high concurrency with no lock contention; conflict handling is explicit and
  honest; the `version` also powers staleness detection in the board/WS read path.
- **Trade-off (accepted):** clients must implement retry-on-409. This is standard and expected.
  For multi-row critical sections (sprint start/complete) optimistic locking is *not* enough —
  see [ADR-0008](0008-advisory-locks-sprint-ops.md).