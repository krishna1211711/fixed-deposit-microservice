# FD Bounded Context and Distributed Architecture

## Runtime topology

This repository implements the Fixed Deposit bounded context plus small event consumers needed to demonstrate a real distributed architecture.

```text
Angular -> API Gateway -> FD Account Service -> FD MySQL
                              |
                              `-> transactional outbox -> Kafka fd.lifecycle.v1
                                                            |-> Notification Service -> notification_db / Mailpit
                                                            |-> Reporting Service ----> report_db
                                                            |-> Audit Service --------> audit_db
                                                            `-> Accounting Service ---> accounting_db
```

Every service owns its database. Reporting, notification, audit and accounting have no FD-database credentials and never join FD tables. Kafka delivery is at-least-once, so each consumer persists `event_id` in its own inbox before changing its local model.

## FD ownership

The FD service owns:

- opening requests and maker-checker decisions;
- booked FD accounts and immutable contracted terms;
- append-only FD financial transactions;
- daily interest accrual, capitalization and payout scheduling;
- maturity, renewal and premature closure state transitions;
- period statements;
- banking business date, batch claims, idempotency records and transactional outbox events.

It stores `customer_id`, `product_code` and payout-account references as integration identifiers. It stores customer-name/category and product/rate/rule snapshots only as immutable evidence of the contract that was booked.

## External boundaries and demo adapters

Identity, Customer and Product/Pricing are independent banking capabilities. The FD core accesses Customer and Product through ports, not repositories. Two adapter modes are available:

- `local-demo` keeps deterministic users, customer eligibility and products in the FD application so the college demonstration starts with one command;
- `remote` binds the same ports to team-owned Customer and Product APIs by configuration, without changing FD business logic.

Authentication and product HTTP controllers are also conditional on local mode. In a strict integrated deployment the gateway routes `/api/auth/**` to Identity Service and `/api/product/**` to Product/Pricing Service. The local master tables are demo adapters, not part of the production FD data contract and have no foreign keys to `fd_accounts`.

## Reliable event flow

The FD command, transaction and outbox row are written in the same local database transaction. The outbox relay publishes the versioned envelope to Kafka and retries failures. Events carry `eventId`, `eventType`, `schemaVersion`, `aggregateType`, `aggregateId`, `occurredAt`, `businessDate`, `correlationId`, `causationId` and payload data.

`FD_TRANSACTION_RECORDED` is the accounting integration event. It contains the immutable reference, type, amount, currency and GL mapping; Accounting Service converts it into its own journal entry. Lifecycle events drive the reporting, notification and distributed-audit consumers. No local Spring application-event mechanism is used as a substitute for Kafka delivery.

## Business date and batch safety

Interest accrual, maturity and statement batches use the persistent Banking Clock rather than the host date. The Beginning-of-Day scheduler catches up every missed date sequentially and advances the clock only when that day's jobs succeed. `fd_job_executions` claims a unique job/business-date pair, records a batch UUID, initiator, source, counts and error state, and therefore acts as a database-backed distributed lock. Account/date constraints and immutable references provide a second idempotency layer.

Time Travel is disabled by default. In the local sandbox it can move only forward and executes each intervening business date sequentially through the same production batch functions before advancing the Banking Clock. It is a test simulator, not event sourcing and not an ordinary banking feature.

## State model

An opening request has `PENDING_CHECKER`, `APPROVED`, `REJECTED` or `CANCELLED`. A booked FD aggregate then has `ACTIVE`, `CLOSED`, `PREMATURE_CLOSED` or `RENEWED`. Request/approval states are deliberately kept on the command ledger rather than pretending that an FD account exists before approval.

`principal_amount` is the original deposit. `current_balance` is the interest-bearing FD balance and changes only when interest is capitalized. `accrued_interest` is a cached current total derived from immutable daily interest rows and remains outside current balance until capitalization or payout.

## Intentionally external capabilities

The FD module publishes payout/settlement intent and preserves references, but a real Savings/Payment Service must perform the actual customer-account debit or credit and return success/failure. Enterprise KYC, tax/TDS, bank-wide reconciliation and a real SMS/WhatsApp provider are also outside this bounded context. They must integrate by versioned APIs/events and must never receive direct FD-database access.

This separation is deliberate microservice design, not unfinished FD calculation logic.
