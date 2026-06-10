# ADR-0010: WebSocket/STOMP + Redis relay + event replay

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
We need to broadcast board state changes (`issue_created`, `issue_updated`, `issue_moved`,
`comment_added`, `sprint_updated`), track presence, and support **reconnection with missed-event
replay**. With a horizontally scaled app tier ([ADR-0004](0004-redis-roles.md)), a client may be
connected to a different instance than the one that processed the mutation.

## Decision
Use **Spring WebSocket with the STOMP** subprotocol. Clients subscribe to per-project topics
(e.g. `/topic/projects/{id}/board`). The outbox relay publishes each event to **Redis pub/sub**;
every app instance is subscribed and forwards to its locally connected STOMP sessions. Each event
carries the per-project `seq` from `domain_event_log`; clients track the last `seq` they saw and, on
reconnect, call a **replay** endpoint to fetch everything after it.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **STOMP over WS + Redis relay + log replay (chosen)** | Standard client libs; topic/subscription semantics built in; works across instances; durable replay from the event log | STOMP + Redis to operate |
| Raw WebSocket | Full control, minimal deps | Hand-roll subscriptions, framing, broker relay; more code, more bugs |
| Server-Sent Events (SSE) | Dead simple, HTTP-native | One-way only (weak for presence/acks); reconnection replay is coarse |
| Sticky sessions (no relay) | No pub/sub needed | Fragile; breaks on rebalance/scale; doesn't truly decouple |
| In-memory replay buffer | No DB read for replay | Lost on restart; bounded window — misses long disconnects |

## Consequences
- **Positive:** real-time delivery that survives horizontal scaling; presence via Redis; **durable**
  replay (the event log already exists for the outbox, so replay is "free").
- **Trade-off (accepted):** STOMP + Redis add moving parts, and delivery is at-least-once (clients
  dedupe by `seq`). We implement broadcast + replay for the board topic; presence
  is a thin Redis-set layer documented in the [realtime LLD](../lld/realtime-sync.md).