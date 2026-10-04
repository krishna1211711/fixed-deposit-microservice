# Current FD Scope and Future Core-Banking Architecture

## What this repository implements

This repository is the independently deployable Fixed Deposit bounded context, not a simulation of every core-banking domain inside one service.

- Angular UI and API gateway with JWT role enforcement.
- Java 21 FD service owning FD products, booked accounts, interest lifecycle, transactions, statements and operational jobs.
- MySQL database owned only by the FD service.
- Transactional outbox publishing versioned FD lifecycle events to Kafka.
- Independent notification service with its own MySQL database and Mailpit delivery.
- Independent report service that calls authenticated FD APIs and never queries FD tables.
- CUSTOMER, BANK_OFFICER and ADMIN roles.
- Product administration, multi-currency rules, ACTUAL/365 daily accrual, scheduled capitalization and payout, maturity instructions, renewal, premature closure and idempotency.

## What must not be claimed as implemented

The following are wider core-banking integrations or future services: external KYC, real savings-account debit, central GL posting, checker/branch-manager approval, immutable enterprise audit, BOD/EOD orchestration, reconciliation, tax/TDS, transaction reversal, SMS/WhatsApp and event sourcing.

The local Time Travel utility processes the FD lifecycle forward through a chosen business date. It is not historical reconstruction and is not event sourcing.

## Intended integration boundaries

```text
Customer/KYC Service ----- synchronous API or customer events -----+
Account/Payment Service -- debit result events --------------------+-- FD Service
Workflow Service --------- approval result events -----------------+
                                                                  |
                                                                  +-- FD lifecycle events --> Kafka
                                                                                               |-- Notification
                                                                                               |-- Audit
                                                                                               |-- Reporting
                                                                                               `-- Reconciliation
```

`customer_id`, product identifiers owned elsewhere, settlement-account identifiers and external transaction references are integration identifiers. Cross-service database joins and cross-service foreign keys are prohibited.

## Recommended future sequence

1. Add a separate maker-checker workflow capability with BANK_OFFICER as maker and a distinct CHECKER identity.
2. Integrate with the owning team's Customer/KYC API; do not duplicate their customer tables.
3. Integrate with Account Service using an idempotent debit command and debit-result event before FD activation.
4. Publish accounting commands to a central GL service while retaining the FD transaction reference.
5. Add audit and reconciliation consumers without changing the FD transaction boundary.
6. Add tax and reversal workflows only after the owning services and contracts are agreed across teams.

This preserves the current working module while giving each future capability a clean extraction and integration path.
