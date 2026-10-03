# FD Backend Remediation Evidence — 2026-10-03

This file records the reproducible baseline and the first remediation milestone on branch `codex/fd-backend-remediation`.

## Preserved baseline

- Recovery commit: `8e568ecd chore: preserve realistic FD lifecycle baseline`
- Scope preserved before remediation: FD service, gateway, notification service, report service, Angular UI, Docker Compose, Kafka, MySQL, Mailpit, documentation, and tests.
- Repository secret scan found only documented local/demo defaults; no real credential was committed during this milestone.

## Platform baseline and migration

| Item | Verified result |
|---|---|
| Required Java release | Java 21 |
| FD service | Spring Boot 3.5.16, Spring Framework 6.2.19 |
| API gateway | Spring Boot 3.5.16, Spring Cloud 2025.0.3 |
| Container JVM | Eclipse Temurin 21.0.12.1 |
| Compiled class version | Java class major version 65 (Java 21) |
| Database | MySQL 8.0.46 |
| Schema management | Flyway; 18 migrations validated, schema version 18 |
| Event broker | Apache Kafka 3.9.1 |

The gateway configuration was moved to the Spring Cloud 2025 `spring.cloud.gateway.server.webflux` property namespace. The compatibility-migration warning no longer appears at startup.

## Security correction

Public registration can no longer select an application role:

- `role` was removed from the public registration DTO.
- Self-registration always persists the `CUSTOMER` role.
- An attempted `role: ADMIN` payload is rejected as an unsupported request field.
- Malformed or unsupported JSON request bodies return HTTP 400 instead of HTTP 500.
- The integration test verifies that the rejected username is not persisted and that a valid self-registration is stored as `CUSTOMER`.

## Build and test evidence

| Verification | Result |
|---|---|
| FD Maven test suite | 36 tests passed; 0 failures, 0 errors, 0 skipped |
| Gateway Maven test/build | Passed |
| Angular production build | Passed |
| Python service compilation | Passed |
| FD container image | Built successfully with Java 21 |
| Gateway container image | Built successfully with Java 21 |
| Docker Compose startup | All eight services started |
| FD health | `GET http://localhost:8080/actuator/health` returned `UP` |
| Gateway health | `GET http://localhost:9090/actuator/health` returned `UP` |
| Gateway-to-report route | `GET http://localhost:9090/reports/health` returned `UP` with database `UP` |

FD test breakdown at this milestone:

- `FdModuleIntegrationTest`: 4
- `AccountNumberGeneratorTest`: 3
- `FdBusinessRulesTest`: 4
- `InterestCalculationHelperTest`: 5
- `StatementGenerationJobTest`: 1
- `FdAccountServiceTest`: 2
- `InterestLifecycleServiceTest`: 2
- `MaturityServiceTest`: 3
- `ProductServiceTest`: 7
- `ReportServiceTest`: 2
- `WithdrawalServiceTest`: 3

After the immutable-term snapshot correction, the FD Maven suite contains 36 passing tests with zero failures, errors, or skips.

## Immutable booked terms

Flyway migration V18 snapshots the following product rules into each FD account when the deposit is booked:

- day-count convention;
- whether premature closure is allowed;
- the premature-closure penalty percentage.

Existing account rows were backfilled from their products. MySQL verification confirmed all three columns are non-null, schema version 18 is successful, and the existing demonstration FDs hold `ACTUAL_365`, premature closure enabled, and their booked 1.00% penalty. Withdrawal processing now reads the account snapshot instead of the mutable product definition, so a later product edit cannot retroactively change an active FD contract.

## Container build correction

The Java Dockerfiles no longer run `mvn dependency:go-offline`, which downloaded a large unrelated Maven plugin catalog. They now use a BuildKit cache mount for `/root/.m2` and run a non-interactive package build. A repeat gateway image build completed its Maven package phase in under five seconds with the cache warm.

Both Java images now provide image-level HTTP health checks using BusyBox `wget`, which is available in the Alpine runtime image.

## Known follow-up work

This milestone is not the end of the backend remediation. The next phases remain tracked in `BACKEND_FIRST_INDUSTRY_IMPLEMENTATION_PLAN.md`, including stronger idempotency and locking, financial precision and immutable term snapshots, transactional outbox delivery, service-owned data boundaries, contract/integration tests, observability, and final demonstration artifacts.
