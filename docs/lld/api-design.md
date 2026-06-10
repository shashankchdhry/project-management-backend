# LLD: API Design & Conventions

> **Implementation status.** Built: the RFC-9457 error model, correlation IDs, `/api/v1`
> versioning, and the endpoints for auth, issues (create/get/update/transition), move-to-sprint,
> board, search, activity, sprints (list/create/start/complete), and event replay. Designed, not
> in code: comments, watchers, a notifications read API, and idempotency-key handling. Code:
> `adapter/in/web`.

- **Related:** [ADR-0012 versioning](../adr/0012-api-versioning.md), [ADR-0009 idempotency](../adr/0009-idempotency-keys.md), [concurrency LLD](concurrency.md), [security LLD](security.md)

This is the contract every controller follows. It exists so that error shapes, pagination,
idempotency, versioning, and validation are uniform across the API rather than per-endpoint.

---

## 1. Principles

1. **Resource-oriented** REST over JSON. Nouns in paths, verbs as HTTP methods.
2. **Versioned at the URI** (`/api/v1`), additive-only within a version ([ADR-0012](../adr/0012-api-versioning.md)).
3. **DTOs at the edge.** Controllers accept/return web DTOs; mappers translate to/from domain
   objects. The domain is never serialized directly ([ADR-0001](../adr/0001-hexagonal-architecture.md)).
4. **Consistent errors** as RFC 9457 `application/problem+json`.
5. **Every response carries `X-Correlation-Id`** (see [observability LLD](observability.md)).
6. **Mutations are idempotent** when given an `Idempotency-Key`.

---

## 2. Resource & endpoint map

All under `/api/v1`. Auth required except `/auth/login` and `/actuator/health/*`.

| Method | Path | Purpose | Success |
|--------|------|---------|---------|
| POST | `/auth/login` | Obtain JWT | 200 |
| POST | `/projects/{key}/issues` | Create issue | 201 |
| GET | `/projects/{key}/board` | Board state (read model) | 200 |
| GET | `/projects/{key}/issues` | List/filter issues (keyset) | 200 |
| GET | `/issues/{key}` | Get one issue | 200 |
| PATCH | `/issues/{key}` | Partial update (optimistic lock) | 200 |
| POST | `/issues/{key}/transitions` | Move status through workflow | 200 |
| GET | `/issues/{key}/transitions` | Allowed transitions from current status | 200 |
| PUT | `/issues/{key}/sprint` | Move issue to a sprint (or backlog) | 200 |
| GET/POST | `/issues/{key}/comments` | List / add threaded comment | 200/201 |
| POST/DELETE | `/issues/{key}/watchers` | Watch / unwatch | 204 |
| GET | `/projects/{key}/sprints` | List sprints | 200 |
| POST | `/projects/{key}/sprints` | Create sprint | 201 |
| POST | `/sprints/{id}/start` | Start sprint (advisory lock) | 200 |
| POST | `/sprints/{id}/complete` | Complete sprint + carry-over | 200 |
| GET | `/projects/{key}/activity` | Activity feed (keyset, filterable) | 200 |
| GET | `/search` | Full-text + structured query | 200 |
| GET | `/notifications` | My notifications (keyset) | 200 |
| GET | `/projects/{key}/events?after={seq}` | WS missed-event replay | 200 |

The REST endpoints map 1:1 to the resource model (version-prefixed).

---

## 3. Standard request/response conventions

### Versioning
URI prefix `/api/v1`. Within `v1`, only additive changes (new optional request fields, new
response fields, new endpoints). Breaking changes → `/api/v2`. The domain stays version-agnostic;
versioning lives entirely in the web adapter (DTOs + controllers).

### Validation
- **Syntactic** validation (types, required, ranges, formats) via Bean Validation on DTOs →
  **`400 Bad Request`** with field-level errors.
- **Semantic / domain** validation (e.g. illegal workflow transition, WIP limit) happens in the
  domain → **`422 Unprocessable Entity`** with a domain reason. This 400-vs-422 split is deliberate
  and consistent.
- Unknown JSON properties are rejected (`FAIL_ON_UNKNOWN_PROPERTIES`).

### Pagination (keyset / cursor)
Lists that can grow without bound (issues, activity feed, search, notifications) use **cursor
pagination** — never offset.

```
GET /projects/PROJ/activity?limit=50&cursor=eyJ0cyI6...   (opaque, base64 of (ts,id))
```
```jsonc
{
  "items": [ /* ... */ ],
  "page": { "limit": 50, "nextCursor": "eyJ0cyI6...", "hasMore": true }
}
```
`hasMore` is computed by fetching `limit + 1` rows (no expensive `COUNT(*)`). Ordering is stable
on `(sort_key, id)`.

---

## 4. Error model (RFC 9457 `problem+json`)

A single `@RestControllerAdvice` maps a **typed exception hierarchy** to a consistent body:

```jsonc
{
  "type": "https://errors.pm-platform/issue-version-conflict",
  "title": "Issue was modified concurrently",
  "status": 409,
  "detail": "Issue PROJ-123 is at version 4; you submitted version 3.",
  "instance": "/api/v1/issues/PROJ-123",
  "correlationId": "c1f1d0e2-...",
  "errors": [ { "field": "version", "message": "stale" } ],   // optional, for 400/422
  "current": { /* for 409: authoritative current state so the client can merge */ }
}
```

| Exception (domain/app) | HTTP | When |
|------------------------|:----:|------|
| `ValidationException` (Bean Validation) | 400 | Malformed request |
| `AuthenticationException` | 401 | Missing/invalid token |
| `AccessDeniedException` | 403 | Authenticated but lacks role / not a member |
| `ResourceNotFoundException` | 404 | Unknown issue/project/sprint |
| `OptimisticLockConflictException` | **409** | Stale `version` — body includes `current` |
| `IdempotencyConflictException` | 409 | Key reused with a different request |
| `WorkflowTransitionException` | **422** | Illegal transition / failed guard — body lists `allowedTransitions` |
| `BusinessRuleException` | 422 | Other domain rule violation (e.g. WIP limit) |
| `RateLimitExceededException` | 429 | Over limit; includes `Retry-After` |
| (uncaught) | 500 | Logged with correlation id; no internals leaked |

The 409 (stale version) and 422 (illegal transition) responses are first-class, carrying the
extra fields (`current`, `allowedTransitions`) clients need to recover.

---

## 5. Concurrency & idempotency headers

| Header | Direction | Meaning |
|--------|-----------|---------|
| `If-Match: "4"` (or `version` in body) | request | Expected entity version for optimistic locking ([ADR-0007](../adr/0007-optimistic-locking.md)) |
| `ETag: "4"` | response | Current entity version |
| `Idempotency-Key: <uuid>` | request | Safe-retry token for mutations ([ADR-0009](../adr/0009-idempotency-keys.md)) |
| `X-Correlation-Id` | both | Request tracing id |
| `Retry-After` | response | On 429 |

Mechanics of optimistic locking and idempotency live in the [concurrency LLD](concurrency.md).

---

## 6. Authentication (summary)

Stateless **JWT bearer** tokens (`Authorization: Bearer <jwt>`). `POST /auth/login` issues them;
Spring Security validates on every request and resolves the user. Per-project **roles are resolved
per request** from `project_memberships` (not embedded in the token), so membership changes take
effect immediately. Full design in the [security LLD](security.md).

---

## 7. OpenAPI / Swagger

springdoc generates the spec from controllers + DTO annotations. `GET /v3/api-docs`, UI at
`/swagger-ui.html`. Each error type and the pagination envelope are documented as reusable schemas;
endpoints carry example requests/responses (including the 409 and 422 examples above) so the
generated spec doubles as living API documentation.

---

## 8. Worked examples

**Transition rejected:**
```
POST /api/v1/issues/PROJ-123/transitions   { "to": "Done" }
→ 422 { "title": "Illegal transition", "detail": "Cannot move from 'To Do' to 'Done'.",
        "allowedTransitions": ["In Progress"] }
```

**Concurrent update:**
```
PATCH /api/v1/issues/PROJ-123  If-Match: "3"   { "priority": "HIGH" }
→ 409 { "title": "Issue was modified concurrently", "current": { "version": 4, ... } }
```