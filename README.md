# Jira-like Project Management Platform — Backend

Backend for a project-management platform — issues, sprints, a configurable workflow engine,
real-time board updates, search, and collaboration.

## Highlights

- **Hexagonal architecture** — framework-free domain, ports & adapters
- **CQRS** — denormalized board read model kept in sync by domain events
- **Transactional outbox** — every mutation emits ordered, replayable events (activity feed,
  notifications, WebSocket)
- **Concurrency** — optimistic locking (409), Postgres advisory locks for sprint ops, idempotency
- **Real-time** — STOMP over WebSocket, fanned out across instances via Redis pub/sub, with
  missed-event replay
- **Resilience** — Resilience4j circuit breaker around notifications (board keeps working when the
  notification service is down)
- **Security** — stateless JWT auth, per-project RBAC, Redis rate limiting

## Tech stack

| Concern | Choice |
|---------|--------|
| Language / Build | Java 21, Maven (Spring Boot 3.5.x — see [decisions](docs/decisions.md)) |
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
| [docs/hld.md](docs/hld.md) | High-level design — architecture, data model, subsystems, scaling |
| [docs/decisions.md](docs/decisions.md) | Key design decisions and the reasoning behind them |
