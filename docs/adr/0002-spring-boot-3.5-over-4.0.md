# ADR-0002: Spring Boot 3.5.x over 4.0.x

- **Status:** Accepted
- **Date:** 2026-06-10

## Context
At project start, Maven Central shows the latest Spring Boot GA on the **4.0.x** line, with
**4.1.0-RC1** still a release candidate, and the **3.5.x** line at 3.5.14 (GA, still within its
open-source support window). The application leans on two ecosystem libraries that are not part of
Spring Boot's own dependency management: **springdoc-openapi** (Swagger UI) and **Resilience4j**
(circuit breaker). A build or startup surprise is expensive here.

## Decision
Pin **Spring Boot 3.5.14**. Use springdoc 2.8.x and resilience4j 2.4.0 (`resilience4j-spring-boot3`),
which are proven against the Spring Boot 3 / Spring Framework 6 line.

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| **Spring Boot 3.5.14 (chosen)** | Entire ecosystem (springdoc 2.x, resilience4j `spring-boot3`) battle-tested here; abundant references; lowest risk | Not the newest major; OSS support window closing ~mid-2026 |
| Spring Boot 4.0.x | Latest GA, Spring Framework 7, modern | Resilience4j still ships only a `spring-boot3` starter — Boot 4 autoconfig compatibility is unproven for our stack; springdoc 3.x is newer/less battle-tested; subtle startup issues possible |
| Spring Boot 4.1.0-RC1 | Newest features | **Not GA** — RCs are inappropriate for a production release |

## Consequences
- **Positive:** the build resolved and compiled first try; all the patterns we use (WS, JPA, Redis,
  Actuator, Resilience4j, springdoc) have known-good versions.
- **Trade-off (accepted):** we are one major version behind. For a reliability-first
  build this is the right call; being current is less valuable than being certain it runs.
- **Revisit if:** Resilience4j and springdoc ship mature, widely-used Boot 4 releases — then a
  migration is low-risk and worthwhile.