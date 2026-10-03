# Team Swagger — FD Contract Review

**Reference reviewed:** `C:\Users\krish\Downloads\Swagger_API.yaml`  
**Review scope:** Fixed Deposit module only  
**Status:** Reference contract, not an instruction to implement Authentication, Customer, Calculation, or Product services

## 1. Scope decision

The consolidated Swagger contains Authentication/User, Customer relay, Product/Pricing, and FD Account Management operations. This repository will implement only the FD-owned capability.

Other modules are treated as external systems with versioned contracts:

- Authentication/Identity supplies a validated subject and roles.
- Customer Service resolves the subject to a stable customer number/profile.
- Calculation Service supplies a referenced, immutable calculation result.
- Product Service supplies a published product version and allowed selections.
- FD Service owns the FD account, accepted term snapshot, lifecycle, balances, transactions, statements, local ledger, and integration events.
- Reporting and Notification consume FD events and own their own data.

The FD implementation must run locally even when other groups' services are unavailable. Contract-compatible stubs/WireMock fixtures will be supplied for development and demonstration.

## 2. FD operations present in the team Swagger

| Team operation | Ownership decision | Target treatment |
|---|---|---|
| `POST /api/v1/accounts` | FD Account Service | Supported gateway-facing compatibility API |
| `GET /api/v1/accounts/search` | FD Account Service | Replace generic `idType/value` internally with typed queries; keep compatibility facade if required |
| `GET /api/v1/accounts/{accountNumber}/balances` | FD Account Service | Supported, with explicit current balance and accrued-interest separation |
| `GET /api/v1/accounts/{accountNumber}/transactions` | FD Account Service | Supported with precise event types and pagination |
| `GET /api/v1/accounts/{accountNumber}/withdrawal-inquiry` | FD Account Service | Supported as a non-mutating quote with expiry/reference |
| `POST /api/v1/accounts/{accountNumber}/withdrawal` | FD Account Service | Supported as an idempotent command with settlement status |
| `POST /api/v1/accounts/{accountNumber}/roles` | FD Account Service | Supported only if product rules allow the role and Customer Service validates the customer |
| `POST /api/v1/jobs/run/interest-calculation` | Operations interface | Not public; admin/internal trigger returning a job-run resource |
| `POST /api/v1/jobs/run/maturity-processing` | Operations interface | Not public; admin/internal trigger returning a job-run resource |
| `GET /api/v1/reports/accounts/maturing` | Reporting Service | Projection/query owned by Reporting, not direct FD database access |
| `GET /api/v1/reports/accounts/created` | Reporting Service | Projection/query owned by Reporting |
| `GET /api/v1/reports/accounts/closed` | Reporting Service | Projection/query owned by Reporting |
| `GET /api/v1/auth/verify` | Identity/Gateway | Not FD-owned; exclude from the FD contract |
| `GET /api/v1/auth/me` | Identity/Gateway | Not FD-owned; exclude from the FD contract |

The Auth, User, Customer relay, and Product CRUD paths in the consolidated file will not be implemented by the FD service.

## 3. Contract mismatch with the current repository

The team contract creates an FD using:

```json
{
  "accountName": "My Retirement FD",
  "calcId": 12345
}
```

It derives customer identity from the JWT and fetches calculation/product details from other services.

The current repository's officer-facing request instead accepts customer, product, amount, tenure, branch, currency, categories, compounding frequency, payout frequency, maturity instruction, and start date directly. The current create endpoint is also restricted to bank officers/admins.

This mismatch will be handled without weakening the core model:

1. Introduce a gateway-facing `POST /api/v1/accounts` compatibility controller.
2. Require `Idempotency-Key` and a validated JWT subject.
3. Resolve the subject through `CustomerPort`.
4. resolve `calcId` through `CalculationPort` and obtain an immutable calculation/version reference.
5. Resolve the exact published product version through `ProductPort`.
6. Validate that the calculation, product, currency, amount, tenure, frequencies, payout choice, and customer categories agree.
7. Map those validated values into one internal `OpenFdCommand`.
8. Store the complete accepted terms snapshot and upstream reference IDs.
9. Commit FD state, initial deposit/local-ledger postings, and outbox event atomically.
10. Return `201 Created` with a `Location` header and FD account representation.

The existing direct/assisted account-opening endpoint can remain temporarily as a versioned staff-only API, but it must use the same command handler and invariants. It must not become a second implementation of FD opening.

## 4. Problems in the supplied consolidated Swagger

These are specification issues to correct in the FD-owned contract rather than reproduce:

1. Multiple service URLs are declared globally. In OpenAPI they are alternatives for every operation, not tag-based routing. Each service needs its own OpenAPI document; a gateway catalog can link them.
2. Most FD operations, including manual batch triggers, do not declare security.
3. Two differently named bearer schemes exist: `bearerAuth` and `Bearer Authentication`.
4. FD error responses commonly reuse the successful account/array DTO instead of a stable error schema.
5. Wildcard `*/*` response content types should be explicit `application/json`.
6. `204 No Content` responses incorrectly define response bodies.
7. Empty collection searches should return `200 []`, not `404`.
8. Search endpoints have no pagination and expose a generic `idType/value` query that is difficult to authorize and document safely.
9. Monetary fields are generic `number` values with no scale, precision, currency, or generated `BigDecimal` guarantee.
10. Balance types `FD_PRINCIPAL`, `FD_INTEREST`, and `PENALTY` do not clearly distinguish original principal, current capitalized balance, uncapitalized accrued interest, paid interest, and penalty.
11. The response omits payout frequency, maturity instruction, accrued interest, current balance, last accrual date, last capitalization date, next capitalization date, product version, settlement status, and processing references.
12. The account-creation description says events are published after the response without specifying an outbox/recovery guarantee.
13. Hard-coded localhost service URLs appear in behavioral descriptions rather than environment/configuration documentation.
14. The calculation reference has no expiry, ownership, version, consumed status, or replay semantics.
15. No idempotency header or duplicate-request behavior is specified.
16. Manual jobs return plain strings and do not expose business date, run ID, progress, failures, or duplicate-trigger behavior.
17. Account-holder changes have no authorization policy, product validation, effective dates, audit/version field, or ownership-total invariant.
18. No statement endpoint is present even though statements are a core FD presentation requirement.
19. No API is defined for maturity instruction selection/status, capitalization history, payout status, renewal relationship, or reconciliation.
20. The consolidated contract is version `1.0.0` but has no compatibility/deprecation policy.

## 5. Canonical FD API to publish

The FD service will publish its own OpenAPI document under a versioned gateway base path. The exact prefix can be coordinated with the team; the proposed canonical path is `/api/v1/fd` while retaining compatibility aliases during integration.

### Customer/staff commands

- `POST /accounts` — open an FD from a calculation reference.
- `POST /accounts/{accountNumber}/withdrawal-quotes` — create a time-limited premature-withdrawal quote.
- `POST /accounts/{accountNumber}/closures` — accept a quote and initiate idempotent closure/settlement.
- `POST /accounts/{accountNumber}/holders` — add an allowed holder/nominee/guardian.
- `DELETE /accounts/{accountNumber}/holders/{holderId}` — controlled holder removal where policy permits.

### Customer/staff queries

- `GET /accounts/{accountNumber}` — FD terms and current lifecycle state.
- `GET /accounts` — authorized, paginated portfolio/search.
- `GET /accounts/{accountNumber}/balances` — original principal, current balance, accrued interest, paid interest, and penalty totals.
- `GET /accounts/{accountNumber}/transactions` — paginated business events/financial transactions.
- `GET /accounts/{accountNumber}/statements` — statement periods and summary metadata.
- `GET /accounts/{accountNumber}/statements/{statementId}` — statement detail/download metadata.
- `GET /accounts/{accountNumber}/capitalizations` — capitalization schedule/history.
- `GET /accounts/{accountNumber}/settlements` — authorized payout/closure settlement status.

### Internal operations

- `POST /internal/job-runs/accruals` — schedule one business-date/partition run.
- `POST /internal/job-runs/capitalizations` — schedule due capitalizations.
- `POST /internal/job-runs/maturities` — schedule maturity processing.
- `GET /internal/job-runs/{jobRunId}` — job status and counts.

Internal operations are not routed publicly and require a service/admin scope. A demonstration-only time-travel feature is isolated to a local/demo profile and fully audited.

## 6. Required API conventions

- OAuth2/OIDC bearer tokens with one consistently named security scheme.
- Customer identity comes from a validated token; a public customer request never chooses an arbitrary customer ID.
- Staff acting for a customer must have a privileged scope and create an audit record.
- `Idempotency-Key` is required for money-changing POST requests.
- `X-Correlation-ID` is accepted/generated and returned.
- Monetary values use an exact decimal representation with ISO currency. The generated Java type must be `BigDecimal`.
- Dates are ISO `date`; instants are UTC `date-time`; business date and processing timestamp are separate.
- Errors use RFC 9457 Problem Details with stable `code`, `correlationId`, and field violations.
- Create returns `201`; accepted asynchronous work returns `202`; empty collections return `200 []`.
- Pagination, sorting, maximum page size, and deterministic order are documented.
- Optimistic update endpoints use a version/ETag where applicable.
- The OpenAPI contract is generated/linted and compatibility-checked in CI.

## 7. Database presentation package

For demonstration, documentation must show the FD-owned schema rather than one shared team database.

Required database artifacts:

1. Per-service ER diagram, with the FD diagram showing:
   - `fd_accounts`;
   - accepted term/product snapshot;
   - account holders;
   - accrual records;
   - capitalization records;
   - transactions;
   - balanced ledger entries/posting lines;
   - statements;
   - settlement attempts;
   - idempotency records;
   - outbox events;
   - consumed/inbox messages where applicable;
   - job runs and audit records.
2. A data dictionary containing type, precision, nullability, key, business meaning, and example.
3. Flyway migration timeline and an empty-to-current migration demonstration.
4. Read-only database demonstration credentials in the local/demo profile only.
5. Prepared SQL views/queries for:
   - account terms and current position;
   - accrual versus capitalization;
   - ledger balancing;
   - duplicate/idempotency proof;
   - pending outbox/settlement work;
   - maturity and renewal linkage.
6. Seed data covering each frequency, maturity instruction, currency scale, active/matured/closed state, and failed/retried integration.
7. No direct database access from another microservice; demonstration queries are read-only operator tools.

## 8. Postman/Newman presentation package

The FD deliverable will include a generated or synchronized Postman collection plus environments.

### Files

- `postman/FD-Microservice.postman_collection.json`
- `postman/Local.postman_environment.json` with placeholders, never secrets
- `postman/Team-Integration.postman_environment.json` with placeholders
- `postman/README.md`
- Newman HTML/JUnit output generated in CI, not committed if it contains tokens

### Collection folders

1. Health and readiness.
2. Authentication/token setup reference (calls the external IdP; not implemented by FD).
3. Open FD happy path.
4. Account and portfolio queries.
5. Daily accrual and duplicate-run proof.
6. Monthly/quarterly/half-yearly/yearly capitalization.
7. Statements and transactions.
8. Premature-withdrawal quote and closure.
9. Maturity payout.
10. Renew principal.
11. Renew principal and interest.
12. Settlement pending/failure/recovery.
13. Authorization/ownership negative tests.
14. Validation and Problem Details.
15. Idempotency replay.
16. External-service failure and recovery.

### Automated assertions

- status and content type;
- response schema;
- correlation ID and resource ID capture;
- exact decimal/currency fields;
- state transition;
- current balance versus accrued interest;
- expected transaction type;
- no duplicate transaction after replay;
- customer cannot read another customer's account;
- staff-only/internal endpoints reject customer tokens;
- asynchronous job/settlement reaches an expected terminal or pending state.

The collection is presentation support, not the only test layer. Financial correctness remains covered by Java unit/property/integration tests, Testcontainers, contract tests, and full-stack tests.

## 9. Demonstration sequence

The recommended live demonstration is deterministic and can run entirely locally:

1. Start infrastructure and show service health/readiness.
2. Display the service map and separate databases.
3. Obtain a customer token from the local IdP.
4. Use stubbed Customer, Calculation, and Product responses matching the team contract.
5. Open an FD using `calcId` and an idempotency key.
6. Repeat the same request and prove that no second account/deposit was created.
7. Show original principal, current balance, and accrued interest separately in API and database.
8. Run daily accrual twice for the same business date and prove exactly one accrual.
9. Move to a capitalization boundary and show the ledger-balanced transfer from accrued liability to FD balance.
10. Show transaction history and a statement.
11. Demonstrate premature-withdrawal inquiry without mutation, then optionally execute a closure.
12. Demonstrate maturity payout or renewal and repeat the job to prove idempotency.
13. Show Kafka event, outbox/inbox state, notification/report projection, trace/correlation ID, and test evidence.

## 10. Acceptance criteria for team compatibility

- The FD service passes provider contract tests for every FD operation adopted from the team Swagger.
- Team paths can be reached through gateway compatibility routes without leaking other modules into the FD codebase.
- Customer, Calculation, and Product integrations are ports/adapters with WireMock and consumer contract tests.
- Unavailable upstream services produce explicit retryable/non-retryable errors and never create a partial FD.
- No Auth, Customer, Calculation, or Product table is added to the FD database.
- No other service reads the FD database.
- The canonical FD OpenAPI is more precise than the consolidated reference and is the source for generated clients/Postman assets.
- Any intentional deviation from the team Swagger is recorded in a compatibility matrix and agreed with the integration team.
