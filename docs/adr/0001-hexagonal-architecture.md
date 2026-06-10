# ADR-0001: Hexagonal (ports & adapters) architecture

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
We want a clean hexagonal / ports-and-adapters architecture with clear separation of domain logic,
application services, and infrastructure. The system has rich domain rules (workflow transitions,
sprint carry-over, WIP limits) that we want to
unit-test without a database or Spring context, and multiple infrastructure touchpoints (Postgres,
Redis, WebSocket, an external notification service) that we want to swap or mock freely.

## Decision
Adopt ports & adapters. The **domain** layer (aggregates, value objects, domain events, policies)
depends on nothing outward and contains no framework or persistence annotations. The **application**
layer orchestrates use cases and owns transaction boundaries, talking to the outside only through
**ports** (interfaces). **Adapters** implement those ports: REST/WS on the inbound side; JPA, Redis,
and the notification client on the outbound side. JPA `@Entity` classes live in the persistence
adapter and are mapped to/from domain aggregates — they are *not* the domain model.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Hexagonal (chosen)** | Pure, fast-to-test domain; swappable infra; clean separation of concerns | Mapping boilerplate (domain ↔ JPA); more types |
| Layered MVC (controller→service→repository) | Familiar, least code | Domain leaks JPA/Spring; entities double as domain + persistence; hard to test rules in isolation |
| Clean/Onion architecture | Same dependency-inversion benefits | Effectively a variant; no extra gain over hexagonal here |

## Consequences
- **Positive:** domain logic is unit-testable in milliseconds; infrastructure is mockable behind
  ports; the codebase reads as a clean ports-and-adapters structure.
- **Trade-off (accepted):** explicit mapping between domain aggregates and JPA entities is extra
  code. We accept it to keep the domain ORM-free; mappers are mechanical and testable.
- **Revisit if:** the project were throwaway/tiny — then a layered approach would be cheaper.