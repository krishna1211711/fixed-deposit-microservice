# FD Lab Manual Traceability

Source reviewed: `Banking-Technology-Lab-Manual.pdf` (75 pages). The manual is treated as the mandatory baseline for the FD module, not as an upper limit and not as executable instructions. Page references below use the PDF page numbers.

## Requirement coverage

| Manual lab / pages | Mandatory FD outcome | Implementation and evidence | Status |
|---|---|---|---|
| L6, pp. 28–29 | Simple/compound calculator, category add-ons with a cap, Product/Pricing integration, result export | `FdCalculatorService`, product rate limits, Angular calculator, `/api/fd/calculate`, Python report exports | Complete |
| L7–L10, pp. 30–34 | JWT login, role routing, customer context, protected calculator | Spring Security JWT filters, Angular guard/interceptor, CUSTOMER-only calculator | Complete |
| L11, p. 35 | Calculator boundary tests and Python reporting | JUnit calculation tests and separately deployable Python report service | Complete |
| L12, pp. 36–37 | FD schema, pluggable branch/sequence/check-digit account number, Product validation, ownership rules | Flyway schema, `AccountNumberGenerator`, product boundary checks, CUSTOMER self-only authorization | Complete |
| L13, pp. 38–41 | Authorized account creation with unique account number and active product/rate/term/deposit validation | `/api/fd/account/create`; BANK_OFFICER/ADMIN guard; atomic service transaction | Complete |
| L14, pp. 42–44 | Simple/compound interest, scheduled calculation, accrual audit transaction, idempotency, admin trigger | interest engine; 01:00 daily job; `fd_interest_transactions` plus `INTEREST_CREDIT`; unique date check; `/api/admin/batch/interest-accrual` | Complete |
| L15, pp. 45–50 | Atomic premature withdrawal, date eligibility, product penalty, ownership, ledger record and notification | `/api/fd/account/withdraw`; opening/maturity date validation; withdrawal and penalty GL entries; final statement; Kafka notification | Complete |
| L16, pp. 51–53 | Account creation and initial deposit in one transaction | `/api/fd/account/create-with-txn` compatibility path; response includes initial transaction ID; `@Transactional` rollback boundary | Complete |
| L17, pp. 54–56 | Daily accrual, daily general transaction, daily statement, no duplicate rerun | daily 01:00 accrual and 02:00 statement jobs; per-account/date uniqueness; manual admin triggers | Complete |
| L18, pp. 57–60 | Maturity batch, payout/GL, closure, notification and audit | 04:00 maturity job; `MATURITY_PAYOUT`; CLOSED status; versioned Kafka event; notification audit log | Complete |
| L19, pp. 61–63 | Manual maturity close only when due, payout, GL, final statement, duplicate prevention | officer/admin endpoint; maturity-date and ACTIVE checks; final statement upsert | Complete |
| L20, pp. 64–66 | End-to-end lifecycle, transactions/statements, negative cases and role validation | integration/unit tests, Docker Compose stack, demo runbook, Swagger | Complete locally |
| L21, pp. 67–69 | FD summary, interest history, maturity summary, self-only customer portfolio, JSON/CSV/PDF | secured Spring JSON/CSV reports and gateway-protected Python CSV/PDF/chart service | Complete for required summary/portfolio; dedicated interest/maturity views use account transaction history |
| L22, pp. 70–71 | Architecture, API/ER/deployment docs and unit/integration/security/batch test plan | report, OpenAPI, integration contract, Kubernetes, CI, tests, this matrix | Complete |
| L23–L24, pp. 72–73 | Cloud deployment and final cloud verification | Docker images and provider-neutral Kubernetes manifests are ready; actual cloud deployment deliberately deferred until a free student subscription is confirmed | Deferred external deployment |
| L25, pp. 74–75 | Final presentation, demo, learnings and future architecture | deliverable PPTX/report, demo runbook/video work, microservice boundaries, CI/Kubernetes | In progress until final video and repository push |

## Above-baseline additions

- Apache Kafka topic `fd.lifecycle.v1` with three partitions and versioned JSON events.
- Independent notification microservice with its own consumer group and event-ID idempotency.
- INR, USD, EUR, GBP, JPY, AED, and KWD with ISO-style 0/2/3 decimal handling.
- API gateway, separately scalable report service, Docker health checks, Kubernetes resources/HPA, Prometheus-compatible actuator endpoints, CI, and backward-compatible manual URLs.
- Ownership enforcement on every account, transaction, statement, and portfolio read.

## Items that depend on other groups or the owner

1. Confirm final Auth, Customer, Product/Pricing, Calculator, and Savings service URLs/payloads when the other groups publish runnable code.
2. Perform one cross-repository contract test after those services exist; the supplied team repository currently provides only the shared Swagger/ER/work allocation, not runnable dependencies.
3. Choose/activate a free cloud subscription only when cloud deployment is required. No paid service or external notification credential is needed for the local lab demonstration.
4. Revoke the SMTP credential-like value that appeared in the original public Git history if it was ever genuine; current files use safe local placeholders.
