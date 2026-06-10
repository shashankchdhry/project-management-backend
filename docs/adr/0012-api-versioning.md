# ADR-0012: URI-prefix API versioning (`/api/v1`)

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
We need an API versioning strategy with backward compatibility: a scheme
that is obvious to consumers, easy to test/document, and that lets us evolve without breaking
existing clients.

## Decision
Version in the **URI path**: all endpoints live under `/api/v1`. Within a version we make only
**additive, backward-compatible** changes (new optional fields, new endpoints). A breaking change
introduces `/api/v2`, and `v1` continues until deprecation. OpenAPI docs are generated per version.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **URI prefix `/api/v1` (chosen)** | Visible and unambiguous; trivial to route, test, cache, and document; easy for consumers to pin | URL "purists" object; some duplication when v2 forks |
| Header / media-type negotiation (`Accept: application/vnd.app.v1+json`) | Clean URLs; RESTful | Harder to test/browse/curl; easy to get wrong; poorer discoverability |
| No versioning | Less ceremony now | Guarantees a painful breaking change later; no compatibility story |

## Consequences
- **Positive:** clients pin a version explicitly; the contract is discoverable in the URL and in
  per-version Swagger; backward compatibility is a simple, enforceable rule.
- **Trade-off (accepted):** when `v2` arrives, some handlers/DTOs are duplicated or adapted. We keep
  the domain version-agnostic and confine versioning to the web adapter (DTOs + controllers), so the
  cost stays at the edge.