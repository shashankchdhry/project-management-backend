# ADR-0013: Single-table issue-type modeling

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
Issues come in five types — Epic, Story, Task, Bug, Sub-task — with a hierarchy (Epic → Story →
Sub-task). They share the vast majority of their attributes (title, status, assignee,
priority, sprint, comments, watchers, custom fields) and are queried together constantly (board,
search, activity feed).

## Decision
Store all issue types in **one `issues` table** with a `type` discriminator column and a
self-referencing `parent_id` for the hierarchy. The *rules* about which type may parent which (e.g.
a Sub-task's parent must be a Story/Task) are enforced in the **domain layer**, not the schema,
because they are policy.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Single table + discriminator (chosen)** | Flat, fast board/search/feed queries; one set of indexes; polymorphic queries are trivial | A few type-specific columns are nullable; type rules live in code |
| Joined inheritance (base + per-type tables) | Normalized; type-specific columns isolated | Every read/write joins; board/search get more complex and slower |
| Table-per-type | Full isolation | No simple polymorphic query across types — exactly what the board/search need |

## Consequences
- **Positive:** the dominant access patterns (board, search, feed) read one table with flat
  indexes; adding a type is a new enum value, not a schema fork.
- **Trade-off (accepted):** type-specific invariants are enforced in the domain rather than by the
  database, and a handful of columns are type-dependent/nullable. This matches the hexagonal stance
  that policy belongs in the domain ([ADR-0001](0001-hexagonal-architecture.md)).