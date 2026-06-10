# ADR-0004: Redis for cache, rate limiting, presence, and pub/sub

- **Status:** Accepted
- **Date:** 2026-06-10
- **Implementation:** Built — pub/sub fan-out and token-bucket rate limiting. Not yet implemented — the board-state cache (the board is served from the `issue_board_view` read model, not a cache) and presence sets.

## Context
Several features need *shared, cross-instance* state if the app tier is to scale horizontally:
a hot board cache, rate limiting per user/endpoint, presence tracking (who's viewing a board), and
a way to broadcast WebSocket events to clients connected to *different* app instances. In-process
structures can't do the cross-instance parts.

## Decision
Run **Redis** as the shared backplane for four roles: (1) board-state cache *(not yet implemented)*, (2) token-bucket rate
limiting, (3) presence sets with TTL *(not yet implemented)*, (4) **pub/sub relay** that fans domain events out to every
app instance's WebSocket sessions.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Redis (chosen)** | One tool covers cache + rate-limit + presence + fan-out; enables stateless, multi-instance app tier | Another stateful dependency to run |
| In-process (Caffeine cache, local rate limiter) | Zero extra infra, lowest latency | Per-instance cache (low hit rate, no invalidation across nodes); rate limits not global; **WS events can't reach clients on other instances** |
| Kafka for fan-out | Durable, high throughput | Overkill for board events; heavier ops; we still need a cache/rate-limit store |

## Consequences
- **Positive:** the app tier becomes truly stateless and horizontally scalable; WebSocket delivery
  works regardless of which instance a client connects to.
- **Trade-off (accepted):** one more system to operate. Acceptable because everything stored in
  Redis is derived/ephemeral and reconstructible (cache, presence) — losing Redis degrades, not
  corrupts.
- **Revisit if:** durability/ordering guarantees for fan-out become a hard requirement → consider
  Redis Streams or Kafka.