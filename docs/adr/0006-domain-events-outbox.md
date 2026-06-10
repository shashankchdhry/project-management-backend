# ADR-0006: Domain events via a transactional outbox

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
Every mutation must drive several side effects: the activity feed/audit trail, notifications, and
WebSocket broadcasts. Two hard constraints: (a) a notification outage must not fail board
operations — when the notification service is down, board operations must keep succeeding, so side
effects must be **decoupled** from the request; (b) we must never have a state change without its
event (or vice-versa), and real-time delivery needs an **ordered, replayable** stream for
missed-event replay.

## Decision
Each mutation raises **domain events** that are written to `domain_event_log` **in the same
transaction** as the state change (the **transactional outbox** pattern). A relay worker then
publishes unrelayed events asynchronously to three consumers: the activity/audit projector, the
notification dispatcher (behind a circuit breaker), and the Redis pub/sub WebSocket relay. Events
carry a per-project monotonic `seq` for ordering and replay.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Transactional outbox (chosen)** | Atomic (event iff state change); decoupled side effects; durable, ordered, replayable; survives consumer outages | Needs a relay worker; delivery is eventual |
| Synchronous in-request side effects | Simplest to write | Couples board op to notification availability (**fails on a notification outage**); partial-failure risk; no replay |
| External broker (Kafka) as source of truth | Scalable, durable | Dual-write problem (DB + broker) unless you *also* use an outbox; heavier infra than warranted |

## Consequences
- **Positive:** correctness (no lost/orphan events), resilience to a notification outage, one stream that simultaneously
  serves audit, notifications, and real-time, plus replay for reconnecting clients.
- **Trade-off (accepted):** consumers are eventually consistent and the relay is an extra moving
  part. For an in-process design the relay is a scheduled poller over the unpublished partial index.
- **Revisit if:** throughput demands a real broker → the outbox feeds Kafka without changing producers.