# Project Status

## Implemented

- Java 21 target, Spring Boot 3.5.16, Flyway, MySQL 8, Kafka 3.9, API gateway, Angular UI, Python report and notification services.
- Realistic FD lifecycle: original principal, current balance, daily `ACTUAL_365` accrual, independent capitalization and payout frequencies, calendar schedules, statements, maturity instructions, renewal, and premature closure.
- Booked terms are snapshotted on each account so later product changes do not rewrite active contracts.
- Precise business events and GL meanings for deposit, accrual, capitalization, payout, maturity, renewal, and premature closure.
- Account opening requires an `Idempotency-Key`; the original response is stored and replayed, while changed-payload reuse returns HTTP 409.
- Accrual, capitalization, payout, maturity, transaction references, and statements have operation-level duplicate protection.
- Scheduled accrual, statement, and maturity jobs use a database claim keyed by job and business date, allowing safe multi-replica scheduling.
- FD state and its Kafka event are committed atomically through `fd_outbox_events`; the relay retries failed publication and recovers stale claims.
- Notification is an independent Kafka consumer with its own MySQL schema, inbox deduplication, retries, delivery audit, and dead-letter publication.
- Reporting is independently deployable and obtains data only through authenticated FD APIs; it has no FD database credentials.
- `fd_accounts.customer_id` and `fd_accounts.product_code` are cross-context identifiers without foreign keys. Internal FD-owned tables retain appropriate FKs.
- Separate FD ER diagram, OpenAPI HTTP contract, AsyncAPI event contract, Docker Compose topology, CI, and demonstration documentation.

## Deliberately local for the college demonstration

- Identity, customer, and product reference implementations remain in the FD application so this repository runs independently. At team integration time their ports must be bound to the other groups' versioned APIs; FD persistence must continue storing identifiers/snapshotted terms only.
- Mailpit is the free local email sink. A real provider is optional and configured only through secrets.
- Docker Compose is the verified zero-cost environment. Kubernetes files are deployment examples, not proof of a live cloud environment.

## External work that cannot be completed inside this repository

- Contract tests against the other groups' running Auth, Customer, Product, and Calculation services require their deployable URLs/builds, issuer/public-key details, and test identities.
- Cloud deployment requires a selected Azure/student subscription and container registry session.
- Professor submission requires final names, registration numbers, section, professor details, and the team's chosen submission channel.

## Credentials needed later

- GitHub browser authentication only when pushing or opening a pull request.
- Team service base URLs and non-production test credentials for cross-group integration.
- Azure/student account and registry login only if cloud deployment is chosen.
- A verified sender plus provider secret only if replacing Mailpit with real email.

Never put passwords, tokens, or cloud keys in source control or chat transcripts; inject them through environment variables or the deployment secret store.
