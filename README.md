# Jira-like Project Management Platform — Backend

Backend for a project-management platform — issues, sprints, a configurable workflow engine,
real-time board updates, search, and collaboration.


## Highlights

- **Hexagonal architecture** — framework-free domain, ports & adapters ([ADR-0001](docs/adr/0001-hexagonal-architecture.md))
- **CQRS** — denormalized board read model kept in sync by domain events
- **Transactional outbox** — every mutation emits ordered, replayable events (activity feed,
  notifications, WebSocket)
- **Concurrency** — optimistic locking (409), Postgres advisory locks for sprint ops, idempotency
- **Real-time** — STOMP over WebSocket, fanned out across instances via Redis pub/sub, with
  missed-event replay
- **Resilience** — Resilience4j circuit breaker around notifications (board keeps working when the
  notification service is down)
- **Security** — stateless JWT auth, per-project RBAC, Redis rate limiting

## Implementation status

The core is built and **tested end-to-end** (32 tests); the remaining capabilities are designed in
[`docs/`](docs/) and marked below — each row is **Built**, **Partial**, or **Designed**
(designed, not yet implemented).

| Capability | Status | Where / note |
|------------|:------:|--------------|
| Relational schema, migrations, demo seed | Built | `db/migration/V1–V6`, `R__seed_demo_data` |
| Issue types + parent/child hierarchy | Built | `domain/issue` |
| Full audit trail (event-sourced) | Built | outbox → `ActivityProjector` → `activity_log` |
| Custom fields per project | Partial | stored as JSONB; **validation against defs** *(designed)* |
| Configurable workflow: statuses, transitions, guards | Built | `domain/workflow`, `TransitionIssueService` |
| Workflow auto-actions (assign reviewer, …) | Designed | modeled in `post_action`; not executed |
| Sprints: start/complete + carry-over + velocity | Built | `SprintService` |
| Sprint list + move issue ↔ sprint/backlog | Built | `SprintController`, `MoveIssueToSprintService` |
| Activity feed API (paginated, filterable) | Built | `ActivityController` / `ActivityFeedService` |
| Notifications | Partial | reporter-on-create + circuit breaker; no read API / full fan-out |
| Threaded comments + @mentions | Designed | `comments` table; no API yet |
| Watchers | Designed | `watchers` table; no API yet |
| WebSocket broadcast + missed-event replay | Built | `RealtimeEventPublisher` (Redis relay), `EventReplayController` |
| Presence tracking | Designed | designed in realtime LLD |
| Full-text search + structured filters + cursor pagination | Built | `IssueSearchAdapter` (Postgres FTS) |
| Hexagonal + CQRS + event-driven outbox | Built | `domain`/`application`/`adapter`; `issue_board_view` |
| `/api/v1` versioning · RFC-9457 errors · correlation IDs | Built | `GlobalExceptionHandler`, `CorrelationIdFilter` |
| Optimistic locking (409) | Built | `@Version` + explicit check |
| Advisory locks (sprint ops) + WIP race | Built | `AdvisoryLockAdapter` |
| Idempotent endpoints | Designed | `idempotency_keys` table; no handler |
| Health probes · metrics · graceful shutdown | Built | Actuator, Micrometer, `application.yml` |
| Structured (JSON) logging · custom metrics | Designed | correlation IDs built; JSON output + WS/outbox gauges not yet |
| Circuit breaker (notification outage) | Built | `ExternalNotificationClient` |
| Pooling · k6 load test · scaling doc | Built | Hikari, `load-test/`, HLD §7 |
| Redis **board cache** | Designed | board served from the read-model table, not a cache |
| RBAC · row-level scoping · validation · rate limiting | Built | `config/security` (**app-level** scoping; PG RLS designed) |

## Tech stack

| Concern | Choice |
|---------|--------|
| Language / Build | Java 21, Maven (Spring Boot 3.5.x — see [ADR-0002](docs/adr/0002-spring-boot-3.5-over-4.0.md)) |
| Web / API | Spring MVC, springdoc-openapi (Swagger UI) |
| Persistence | PostgreSQL + Flyway, Spring Data JPA |
| Cache / pub-sub | Redis (Spring Data Redis) |
| Real-time | Spring WebSocket (STOMP) |
| Auth | JWT (jjwt), Spring Security, BCrypt |
| Resilience | Resilience4j |
| Observability | Spring Boot Actuator, Micrometer + Prometheus |
| Testing | JUnit 5, Testcontainers |

## Running locally

**Prerequisites:** JDK 21, Docker (Postgres + Redis).

```bash
# Everything in containers:
docker compose up --build

# OR run the app against containerized Postgres + Redis:
docker compose up -d postgres redis
./mvnw spring-boot:run
```

Flyway applies the schema and seeds a demo workspace on startup. **Demo logins** (password
`password` for all): `admin@demo.test`, `lead@demo.test`, `member@demo.test`.

| Endpoint | What |
|----------|------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/actuator/health` | Liveness/readiness probes |
| `http://localhost:8080/actuator/prometheus` | Metrics |

### Quick API walkthrough

```bash
# 1. Log in -> JWT
TOKEN=$(curl -s localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"member@demo.test","password":"password"}' | jq -r .token)

# 2. View the seeded board
curl -s localhost:8080/api/v1/projects/WEB/board -H "Authorization: Bearer $TOKEN" | jq

# 3. Create an issue (reporter is taken from the token)
curl -s -X POST localhost:8080/api/v1/projects/WEB/issues \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"type":"STORY","title":"Add dark mode","priority":"HIGH"}' | jq

# 4. Transition it (illegal transitions return 422 with the allowed list)
curl -s -X POST localhost:8080/api/v1/issues/WEB-4/transitions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"to":"In Progress"}' | jq
```

## Tests

```bash
./mvnw test     # 12 domain unit tests + Testcontainers integration tests (needs Docker)
```

Integration tests spin up real Postgres + Redis and cover: the persistence mappings, the full HTTP
slice (incl. **409** optimistic-lock and **422** workflow cases), the board projection +
activity feed, the WebSocket broadcast, the notification **circuit breaker**, and **JWT auth + RBAC**.

## Load test

```bash
k6 run load-test/board-load.js     # 100 concurrent board viewers
```

## Documentation

| Doc | Purpose |
|-----|---------|
| [docs/architecture/hld.md](docs/architecture/hld.md) | High-level design |
| [docs/architecture/data-model.md](docs/architecture/data-model.md) | Schema & ERD |
| [docs/adr/](docs/adr/) | Architecture Decision Records (the "why") |
| [docs/lld/](docs/lld/) | Low-level designs per subsystem |
