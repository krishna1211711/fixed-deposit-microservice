# Verified Test Evidence — 4 October 2026

## Automated verification

- FD microservice: all 36 Maven/JUnit tests passed; compilation targets Java 21.
- Angular UI: production build passed after adding the account-opening idempotency header.
- Notification service: 4 unit tests covering success, inbox deduplication, retry exhaustion/DLQ, and schema rejection passed inside its Docker image; Python compilation also passed.
- Report service: Python bytecode compilation passed.
- Docker Compose configuration validation passed.
- All 17 Kubernetes YAML files passed local YAML parsing. Full `kubectl` schema validation was not available because no Kubernetes API server is connected.
- `git diff --check` reported no whitespace errors (only Windows line-ending notices).

## Running topology

Nine containers are running together and healthy where health checks are defined:

1. Angular UI
2. API gateway
3. FD account service
4. FD MySQL
5. Kafka
6. Notification service
7. Notification MySQL
8. Report service
9. Mailpit

Flyway migrations 19–23 are applied successfully. They create the transactional outbox, remove cross-context FKs, add opening idempotency, add the job execution registry, and archive the old notification table without data loss.

## Live end-to-end proof

An authenticated bank officer opened FD `0010000124` through the gateway with idempotency key `e2e-20261004-boundary-001`.

- The first request created one FD and deposit transaction `3136`.
- Replaying the identical request returned the same FD number.
- Database verification found one account and one `DEPOSIT`, not duplicates.
- Reusing the same key with a different amount returned HTTP 409.
- The idempotency row is `COMPLETED` and points to `0010000124`.
- The same local transaction created outbox event `b2f32a23-324a-4684-b36c-874c4f1edf4b`.
- The relay published that event once and marked it `PUBLISHED`.
- The independent notification consumer stored that event in its own inbox and delivery table with status `SENT`, attempt count 1.
- The report service returned HTTP 200 CSV containing `FD_STD` while holding no FD database credentials.

## Scheduler proof

The statement-generation endpoint was invoked twice for business date `2026-10-03` (the container's UTC date). `fd_job_executions` contains one `STATEMENT_GENERATION` row with status `COMPLETED` and attempt count 1. The duplicate invocation was skipped.

## Data-boundary proof

- `fd_accounts` has no FK to customer or product tables; those values are external references.
- Internal FD tables retain FKs to `fd_accounts` where appropriate.
- The report container receives only `FD_SERVICE_URI`, not FD database settings.
- Notification delivery tables exist only in `notification_db` for active runtime use.
- All 3,134 historic rows from the former shared `notification_log` were preserved in `legacy_notification_log_archive`; the active table name no longer exists.
