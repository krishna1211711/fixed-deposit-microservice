# Verified Test Evidence

Verification was performed locally on 12 September 2026 against the Docker Compose stack.

## Automated builds and tests

- FD microservice: Maven test suite passed, 27 tests.
- API gateway: Maven test/build passed.
- Angular UI: production build passed.
- Notification and report services: Python compilation passed.
- Docker Compose configuration validation passed.

## Running system

All eight containers ran together: Angular UI, API gateway, FD microservice, MySQL, Kafka, notification service, report service, and Mailpit. Health checks passed for the stateful and worker services.

## End-to-end Kafka proof

1. Authenticated through the API gateway as a bank officer.
2. Loaded all seeded products, including INR, USD, EUR, GBP, JPY, AED, and KWD variants.
3. Opened an FD through `POST /api/fd/account/create-with-txn`.
4. The committed transaction published `FD_OPENED` to `fd.lifecycle.v1`.
5. The independent notification consumer processed the event.
6. A `SENT` row with the same unique event ID appeared in `notification_log`.
7. The message appeared in Mailpit.

Verified example account: `0010000023`. Notification health reported connected, a live consumer thread, no last error, and processed messages.

## Idempotency and authorization proof

- Running daily interest twice produced one daily interest row and one general-ledger interest credit.
- Running daily statement generation twice produced one statement for that account/date.
- A customer could use the authenticated calculator compatibility endpoint.
- A customer received HTTP 403 for the officer summary report.
- A bank officer received HTTP 200 for the same report.

These checks demonstrate the manual's required calculation, authentication, account creation, interest, statement, maturity/withdrawal, notification, report, test, and end-to-end themes. The complete mapping is in `LAB_MANUAL_TRACEABILITY.md`.
