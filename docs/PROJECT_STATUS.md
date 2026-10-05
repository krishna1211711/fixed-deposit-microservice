# Project Status

## Implemented

- Java 21 target, Spring Boot 3.5.16, Flyway, MySQL 8, Kafka 3.9, API gateway, Angular UI, and independent Python notification, reporting, audit and accounting services.
- Realistic FD lifecycle: original principal, current balance, daily `ACTUAL_365` accrual, independent capitalization and payout frequencies, calendar schedules, statements, maturity instructions, renewal, and premature closure.
- Booked terms are snapshotted on each account so later product changes do not rewrite active contracts.
- Precise business events and GL meanings for deposit, accrual, capitalization, payout, maturity, renewal, and premature closure.
- Account opening requires an `Idempotency-Key`; the original response is stored and replayed, while changed-payload reuse returns HTTP 409.
- Accrual, capitalization, payout, maturity, transaction references, and statements have operation-level duplicate protection.
- Scheduled accrual, statement, and maturity jobs use a database claim keyed by job and business date, with batch UUID, actor/source, counts and failure details for safe multi-replica scheduling.
- A persistent Banking Clock is shared by account opening and every financial batch. The scheduled Beginning-of-Day catch-up processes every missed date; Time Travel uses the same sequential mechanism and advances the date only after successful batches.
- FD state and its Kafka event are committed atomically through `fd_outbox_events`; the relay retries failed publication and recovers stale claims.
- Notification is an independent Kafka consumer with its own MySQL schema, inbox deduplication, retries, delivery audit, and dead-letter publication.
- Reporting consumes lifecycle events and projects an idempotent read model into `report_db`; it has no FD database credentials or synchronous dependency on FD APIs.
- Distributed audit consumes every lifecycle envelope into `audit_db`; Accounting consumes `FD_TRANSACTION_RECORDED` into immutable double-entry journal rows in `accounting_db`.
- `fd_accounts.customer_id` and `fd_accounts.product_code` are cross-context identifiers without foreign keys. Internal FD-owned tables retain appropriate FKs.
- Separate FD ER diagram, OpenAPI HTTP contract, AsyncAPI event contract, Docker Compose topology, CI, and demonstration documentation.
- Maker-checker opening with CUSTOMER/BANK_OFFICER makers, a separate CHECKER decision, owner snapshots, workflow events, and no ADMIN creation bypass.
- Explicit lifecycle and closure metadata distinguish contractual maturity from actual early closure; legacy CLOSED/zero-balance/future-maturity rows are migrated to PREMATURE_CLOSED.
- Append-only FD audit records cover workflow decisions, product changes, batches, time-travel simulation, maturity, and premature closure.
- Product configuration explicitly chooses whether multiple verified customer-category add-ons stack (subject to the cap) or only the highest eligible add-on applies.
- Staff reports separate active from closed/matured/renewed accounts; customer reports show separate lists.

## Deliberately local for the college demonstration

- Identity, customer, and product reference implementations remain available only as conditional local-demo adapters so this repository runs independently. Remote Customer and Product ports and gateway routes are present; team integration supplies their URLs/contracts while FD persistence continues storing identifiers and immutable snapshots only.
- Mailpit is the free local email sink. A real provider is optional and configured only through secrets.
- Docker Compose is the verified zero-cost environment. Kubernetes files are deployment examples, not proof of a live cloud environment.
- Time travel is a local test/demonstration simulator. It is disabled by default and is not a production banking capability or event sourcing.

## External work that cannot be completed inside this repository

- Contract tests against the other groups' running Identity, Customer and Product/Pricing services require their deployable URLs/builds, issuer/public-key details and test identities.
- Real savings-account debit/payout confirmation, enterprise reconciliation and tax/TDS remain responsibilities of external banking services; the FD service exposes event references and GL intent but cannot move money in another service's ledger.
- Cloud deployment requires a selected Azure/student subscription and container registry session.
- Professor submission requires final names, registration numbers, section, professor details, and the team's chosen submission channel.

## Credentials needed later

- GitHub browser authentication only when pushing or opening a pull request.
- Team service base URLs and non-production test credentials for cross-group integration.
- Azure/student account and registry login only if cloud deployment is chosen.
- A verified sender plus provider secret only if replacing Mailpit with real email.

Never put passwords, tokens, or cloud keys in source control or chat transcripts; inject them through environment variables or the deployment secret store.
