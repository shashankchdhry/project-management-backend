# LLD: Search & Filtering

> **Implementation status.** Built & tested: full-text search (Postgres `tsvector`/GIN),
> structured (query-param) filters, cursor pagination, and row-level scoping. Designed, not in
> code: the JQL-style **string** parser — the code uses query-param filters, which are injection-safe
> and cover the same cases. Code: `IssueSearchAdapter`, `IssueSearchService`, `SearchController`.

- **Related:** [ADR-0011 Postgres FTS](../adr/0011-postgres-full-text-search.md), [data-model §5,§8](../architecture/data-model.md), [security LLD](security.md)

Search is implemented entirely in PostgreSQL ([ADR-0011](../adr/0011-postgres-full-text-search.md)):
full-text relevance and structured filters compose in one parameterized query, consistent with the
live data.

---

## 1. Full-text layer

Stored generated `tsvector` columns ([data-model §5](../architecture/data-model.md)) with **GIN**
indexes:

- `issues.search_vector` = `title` (weight A) ‖ `description` (weight B)
- `comments.search_vector` = `body`

Free text is parsed with **`websearch_to_tsquery('english', :q)`** (accepts quotes, `or`, `-`
safely — no operator-injection from user input). Results are ordered by **`ts_rank`**. A comment
match resolves to its parent issue; issue-level and comment-level hits are unioned and de-duplicated
to the issue, keeping the max rank.

---

## 2. Structured query language

A query like `status = "In Progress" AND assignee = "john"` is supported via a **small, safe
subset** of a JQL-like language plus a free-text `q`.

```ebnf
query      = expr ;
expr       = term { ("AND" | "OR") term } ;
term       = field operator value | "(" expr ")" ;
field      = "status" | "assignee" | "type" | "priority" | "sprint"
           | "label" | "reporter" | "created" | "updated" ;
operator   = "=" | "!=" | "IN" | ">" | ">=" | "<" | "<=" ;
value      = quoted_string | number | date | "(" value {"," value} ")" ;
```

**Compilation pipeline:**
```
raw string ──parse──▶ AST ──validate(field whitelist, type)──▶ SQL WHERE + bound params
```
- **Field whitelist → column map** is the injection defense: only known fields compile, each to a
  known column with a typed, **bound** parameter. User text never reaches SQL as a literal.
- Unknown field / bad operator / type mismatch → **`422`** with a precise message.
- Names resolve to ids (`assignee = "john"` → look up the member, bind their `user_id`).

Two front doors, same engine:
| Style | Example |
|-------|---------|
| Simple params (common cases) | `GET /search?projectKey=PROJ&status=In%20Progress&assignee=john&q=oauth` |
| Structured expression | `GET /search?jql=status%3D%22In%20Progress%22%20AND%20assignee%3D%22john%22` |

---

## 3. Combined query shape

```sql
SELECT i.* , ts_rank(i.search_vector, q) AS rank
FROM issues i, websearch_to_tsquery('english', :q) q
WHERE i.project_id = ANY(:myProjectIds)          -- row-level scoping (security LLD)
  AND (:q = '' OR i.search_vector @@ q)           -- full-text (optional)
  AND i.status_id = :status                       -- compiled structured filters
  AND i.assignee_id = :assignee
  AND (rank, i.id) < (:cursorRank, :cursorId)     -- keyset pagination
ORDER BY rank DESC, i.id DESC
LIMIT :limit + 1;                                 -- +1 to compute hasMore
```

When `q` is empty it's pure structured filtering; when there are no filters it's pure search; the
common case is both.

---

## 4. Indexing & performance

| Need | Index |
|------|-------|
| Full-text | GIN on `issues.search_vector`, `comments.search_vector` |
| Filter by status/assignee (+ board) | btree `(project_id, status_id, …)`, `(assignee_id)` ([data-model §8](../architecture/data-model.md)) |
| Filter by custom field (optional) | GIN on `issues.custom_fields` |
| Keyset pagination | sort keys carried in the cursor |

- **No `COUNT(*)`** for total — we fetch `limit + 1` and report `hasMore` (cheap, O(page)).
- **N+1 avoidance**: result projection selects the display columns directly (assignee name, status
  name) — for board-shaped results we read the `issue_board_view` projection instead of joining.
- **EXPLAIN discipline**: board/search queries are validated with `EXPLAIN (ANALYZE, BUFFERS)` to
  confirm GIN/btree usage and no seq-scan regressions.

---

## 5. Pagination contract

Cursor is an **opaque** base64 of the sort tuple `(rank, id)` (or `(updated_at, id)` for filter-only
queries). Same envelope as everywhere else ([api-design §3](api-design.md)):

```jsonc
{ "items": [ /* issues w/ rank */ ],
  "page": { "limit": 50, "nextCursor": "eyJyYW5rIjowLjg3…", "hasMore": true } }
```

---

## 6. Trade-offs & scope

| Decision | Rationale | Rejected |
|----------|-----------|----------|
| Postgres FTS | One store, consistent, combines with SQL filters | Elasticsearch — extra infra/lag, unjustified at scale ([ADR-0011](../adr/0011-postgres-full-text-search.md)) |
| `websearch_to_tsquery` | Safe parsing of user input | `to_tsquery` — throws on raw user operators |
| Whitelisted mini-JQL | Covers common filter patterns; injection-safe | Arbitrary SQL/filter passthrough — unsafe |
| `limit+1` for hasMore | Avoids costly counts | `COUNT(*)` over large filtered sets |

**Implemented:** full-text over issues+comments, the simple-param filters, keyset pagination, and
row-level scoping are built end-to-end. The full mini-JQL parser is specified here and implemented
for the common operators (`=`, `!=`, `IN`, `AND`) first.