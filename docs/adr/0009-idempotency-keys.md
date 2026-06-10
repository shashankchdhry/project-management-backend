# ADR-0009: Idempotency keys for mutations

- **Status:** Accepted
- **Date:** 2026-06-10
- **Implementation:** Not yet implemented. The `idempotency_keys` table is migrated (`V5`); the request-fingerprint / replay handler is not yet implemented. This ADR records the intended design.

## Context
Idempotent API endpoints must be safe to retry for any mutation without side effects. Clients
retry on timeouts and network blips; without protection, a retried `POST /issues` creates duplicates
and a retried transition double-fires its post-actions.

## Decision
Mutating endpoints are designed to accept an **`Idempotency-Key`** header. On first receipt the service persists the key with
a fingerprint of the request and, once processed, the response status + body
(`idempotency_keys` table). A retry with the same key **replays the stored response** instead of
re-executing. A key reused with a *different* request fingerprint is rejected (`422`). Keys expire
via TTL.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Idempotency-key table (chosen)** | Works for non-idempotent verbs (POST); industry-standard (Stripe-style); also returns the *same* response on retry | Storage + TTL cleanup; one extra lookup per mutation |
| Rely on HTTP method semantics (PUT/DELETE idempotent) | No extra machinery | Doesn't cover POST creates or transition side-effects |
| Natural dedup via unique business keys | No header needed | Only covers cases with a natural key; doesn't replay the original response |

## Consequences
- **Positive:** any mutation is safe to retry; clients get a consistent answer on retry, not a
  second side effect. Composes with optimistic locking (a retry sees its own committed result).
- **Trade-off (accepted):** an extra table and a lookup on the write path, plus TTL housekeeping.
  Negligible relative to the correctness guarantee. Intended scope when built: issue create,
  update, transition, and sprint start/complete.