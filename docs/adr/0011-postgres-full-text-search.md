# ADR-0011: PostgreSQL full-text search over Elasticsearch

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
We need full-text search across issue titles, descriptions, and comments, *combined* with
structured filters (`status = "In Progress" AND assignee = "john"`), proper indexing, and
cursor-based pagination.

## Decision
Implement search with **PostgreSQL full-text search**: `tsvector` stored generated columns on
`issues` (title weighted above description) and `comments`, indexed with **GIN**. Structured filters
compile to SQL `WHERE` clauses on the same query, and full-text rank (`ts_rank`) combines with
filters and keyset pagination — all in one engine, one transaction, consistent with the data.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **PostgreSQL FTS (chosen)** | No extra infra; transactionally consistent with the data; trivially combined with structured SQL filters; good enough at this scale | Relevance tuning less powerful than ES; very large corpora eventually strain it |
| Elasticsearch / OpenSearch | Best-in-class relevance, faceting, scale | Separate system; ingestion pipeline; **eventual consistency** vs the DB; operational weight unjustified at our scale |
| `LIKE` / `ILIKE` | Zero setup | No ranking; no stemming; full scans — not real search |

## Consequences
- **Positive:** one store, strong consistency, and structured + full-text filtering compose
  naturally; achieved with the indexing strategy in [data-model §8](../architecture/data-model.md).
- **Trade-off (accepted):** ranking sophistication and horizontal search scale are below ES. This is
  fine for the target size; ES is the documented scale-out path.
- **Revisit if:** search becomes a primary product surface needing faceting/typo-tolerance/large
  scale → introduce ES fed from the same domain event stream.