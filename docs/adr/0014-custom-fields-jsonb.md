# ADR-0014: JSONB-backed custom fields

- **Status:** Accepted
- **Date:** 2026-06-10
- **Implementation:** Built — per-project definitions (`custom_field_defs`, V4), JSONB value storage on `issues`, and a GIN index (V3). Not yet implemented — write-time validation of values against their definitions.

## Context
We need custom fields per project (text, number, dropdown, date). The set of fields is
defined per project at runtime and varies between projects, so it cannot be fixed columns. Issues
are read far more than custom-field definitions change, and the board/search paths read whole issues.

## Decision
Declare fields per project in `custom_field_defs`; store the **values** in a `custom_fields jsonb`
column on `issues`. On write, values are *designed to be* validated against their definitions
(type, required, dropdown options) — that validator is not yet built. A GIN index on the column
(V3) supports filtering issues by a custom field.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **JSONB on the issue (chosen)** | Issue stays a single row → cheap reads; schema-flexible; GIN-indexable for queries; fits read-heavy board | Validation/typing enforced in app, not by columns; awkward for heavy relational queries on a field |
| EAV (`custom_field_values` rows) | Fully flexible; queryable per value | Every issue read becomes a join + pivot; N rows per issue; painful and slow for the board |
| Wide table with spare/generic columns | Simple typed columns | Rigid (cap on field count/types); wasteful; doesn't model dropdown options |

## Consequences
- **Positive:** custom fields don't bloat the schema or slow down issue reads; definitions and
  values evolve without migrations; aligns with the single-table, read-optimized model.
- **Trade-off (accepted):** type safety and validation move into the application (a custom-field
  validator would check values against defs on write). Complex analytics across a single custom field are
  less natural than a real column — acceptable, since custom fields are project-specific metadata,
  not core reporting dimensions.