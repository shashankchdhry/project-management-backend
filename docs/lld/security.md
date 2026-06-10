# LLD: Security & Access Control

> **Implementation status.** Built & tested: JWT auth (login + filter), per-project RBAC,
> **application-level** row scoping, Redis rate limiting, and Bean-Validation input validation.
> Designed, not in code: Postgres-level RLS policies (app-level scoping enforces the same
> boundary), rich-text sanitization, and a dedicated sensitive-ops audit log (the general activity
> log already captures mutations). Code: `config/security`, `config/SecurityConfig`.

- **Related:** [api-design LLD](api-design.md), [ADR-0004 Redis](../adr/0004-redis-roles.md), [data-model §3](../architecture/data-model.md)

Defense in depth: authenticate at the edge, authorize by role, scope every query by membership,
validate all input, throttle abuse, and audit sensitive actions.

---

## 1. Authentication — stateless JWT

`POST /api/v1/auth/login` verifies credentials (passwords hashed with **bcrypt**) and returns a
signed **JWT** (short-lived access token; refresh token optional). Spring Security validates the
token on every request and establishes the principal.

```jsonc
// JWT claims (minimal)
{ "sub": "<userId>", "wsp": "<workspaceId>", "iat": …, "exp": … }
```

**Roles are intentionally *not* in the token.** Per-project roles are resolved per request from
`project_memberships` so that membership/role changes take effect immediately (no stale-token
window) and the token stays small. Chosen over server-side sessions because the app tier is
stateless and horizontally scaled ([ADR-0004](../adr/0004-redis-roles.md)) — nothing to replicate.

---

## 2. Authorization — RBAC

Four roles, scoped **per project**. A user can be `MEMBER` of one project and `VIEWER` of another.

| Action | Viewer | Member | Project Lead | Admin |
|--------|:---:|:---:|:---:|:---:|
| Read board/issues/search | ✓ | ✓ | ✓ | ✓ |
| Comment, watch | ✓ | ✓ | ✓ | ✓ |
| Create/update/transition issue | | ✓ | ✓ | ✓ |
| Manage sprints (start/complete) | | | ✓ | ✓ |
| Configure workflow, custom fields | | | ✓ | ✓ |
| Manage members / roles | | | ✓ | ✓ |
| Delete project, workspace admin | | | | ✓ |

Enforced in the **application layer** via a permission check at the start of each command (a custom
`PermissionEvaluator` backing `@PreAuthorize("hasProjectRole(#projectKey,'MEMBER')")`, or an explicit
guard in the handler). Domain logic stays role-agnostic; authorization is an application concern.

---

## 3. Row-level security — "see only your projects"

Two layers (defense in depth):

1. **Application scoping (primary).** Every project-scoped query is constrained to the caller's
   memberships. A `ProjectAccessResolver` yields the set of project ids the user may see, injected
   into queries:
   ```sql
   WHERE project_id = ANY(:myProjectIds)   -- search, board, lists, activity, notifications
   ```
   A direct fetch (`GET /issues/{key}`) checks membership of that issue's project → `403`/`404`.
2. **PostgreSQL RLS (documented, optional).** As a backstop, `ENABLE ROW LEVEL SECURITY` with a
   policy keyed on a per-transaction `SET LOCAL app.current_user_id`, so even a query that forgot
   the filter can't leak across tenants. Trade-off: every transaction must set the GUC and it
   complicates pooling — so we make application scoping authoritative and treat RLS as an extra net.

This satisfies "users only see projects they belong to" and the WebSocket subscribe check reuses the
same resolver ([realtime LLD §1](realtime-sync.md)).

---

## 4. Input validation & sanitization

| Vector | Defense |
|--------|---------|
| Malformed/oversized input | Bean Validation on DTOs (`@NotNull`, `@Size`, ranges); reject unknown JSON fields; request size limits → `400` |
| SQL injection | Parameterized queries / JPA everywhere; search compiles to **bound params via a field whitelist**, never string-concatenated SQL ([search LLD §2](search.md)) |
| XSS via stored content (comments, descriptions) | Treat as untrusted text; sanitize/escape on output. API returns raw text + a `contentType` so clients render safely; an allowlist sanitizer strips active markup if rich text is enabled |
| Mass assignment | Explicit DTO→domain mapping; clients can't set server-controlled fields (`version`, `reporter`, timestamps) |
| Custom fields | Validated against `custom_field_defs` (type, required, dropdown options) on write ([ADR-0014](../adr/0014-custom-fields-jsonb.md)) |

---

## 5. Rate limiting (per user + per endpoint)

Redis-backed **token bucket** (so limits are global across instances, [ADR-0004](../adr/0004-redis-roles.md)),
keyed by `userId : endpointClass`. A filter checks/consumes a token; over-limit → **`429`** with
`Retry-After`.

| Endpoint class | Example limit (per user) | Why |
|----------------|--------------------------|-----|
| `auth` | 5 / min | Brute-force resistance |
| `write` (create/update/transition) | 120 / min | Normal interactive use |
| `search` | 30 / min | Heavier queries |
| `read` | 600 / min | Cheap, generous |

Limits are config-driven. (Resilience4j `RateLimiter` or bucket4j-redis; an atomic Redis Lua script
makes check-and-consume race-free.)

---

## 6. Audit logging (sensitive operations)

Sensitive, security-relevant actions are recorded distinctly from the normal activity feed — role
changes, member add/remove, project/workspace deletion, workflow changes, and login events. They
flow through the same event pipeline into `activity_log` (append-only) with `actor_id`,
`correlation_id`, before/after in `changes`, and are flagged/queryable as audit entries. Because the
log is append-only and event-sourced ([data-model §7.3](../architecture/data-model.md)), the audit
trail can't be silently edited.

---

## 7. Transport & secrets

- HTTPS/WSS terminate at the ingress; HSTS + standard security headers.
- CORS allowlist for the SPA origin.
- No secrets in code or VCS — DB/Redis/JWT-signing config comes from environment variables (the
  `application.yml` placeholders); the JWT signing key is an env-provided secret.
- Passwords and tokens are never logged or returned in responses.

---

## 8. Scope

JWT auth, the RBAC permission model, application-level row scoping, input validation, and
Redis-backed rate limiting are implemented end-to-end. PostgreSQL RLS policies and rich-text
sanitization are specified here as follow-ups — application scoping already enforces the
tenancy boundary.