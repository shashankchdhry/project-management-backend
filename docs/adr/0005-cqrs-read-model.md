# ADR-0005: CQRS with a denormalized board read model

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
We want CQRS: separate read models optimized for board view queries vs. write models for command
processing. The board is the hottest read in the system (100 concurrent viewers) and, modeled
naively, each load joins issues → users (assignee) → workflow_statuses →
sprints → custom fields and groups by column — an N+1 magnet. Writes, by contrast, need full
aggregate loading, invariant checks, and locking.

## Decision
Apply **CQRS-lite**: the write side operates on rich domain aggregates (locked, validated); the
read side serves the board from a **denormalized projection** (`issue_board_view`) that is kept up
to date by event projectors subscribed to the domain event stream. Same database, separate
models/tables — *not* separate services and *not* event sourcing.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **CQRS-lite read model (chosen)** | Board read = one flat indexed scan; clean read/write separation; scales reads independently | Eventual consistency window; projector code to maintain |
| Query-time joins (+ optional caching) | Simplest; strongly consistent | Expensive board query; caching alone doesn't fix the cold-read/invalidation cost |
| Full event sourcing | Ultimate auditability, time-travel | Large complexity for little extra benefit here; steep learning/ops cost |

## Consequences
- **Positive:** fast, predictable board reads; the write model stays focused on correctness; reads
  and writes can be tuned/scaled separately.
- **Trade-off (accepted):** the read model is **eventually consistent** (typically sub-second). We
  mitigate with a `version` column on the projection (clients/WS can detect staleness) and
  event-driven cache invalidation. Where strong consistency is required (e.g. immediately after a
  write in the same request), the write side returns the authoritative state.
- **Revisit if:** consistency requirements tighten to "read-your-write everywhere" → read from the
  write model for those paths.