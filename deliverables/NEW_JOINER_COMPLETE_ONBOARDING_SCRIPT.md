# Complete New-Joiner Onboarding Script — Fixed Deposit Banking Platform

This is a ready-to-speak walkthrough. Read the quoted narration in order, and perform the actions shown in **Demonstrate** blocks. The technical names are intentionally included so the listener can connect each business idea to its implementation.

---

## 0. What the listener should know by the end

> “By the end of this walkthrough, you will understand what a fixed deposit is, every financial state our FD passes through, why accrual is different from capitalization and payout, how product rules become account terms, how every browser request travels through the system, which Java function calls which next function, what every database table owns, how Kafka and email notifications work, how security is enforced, how idempotency prevents duplicate money movements, how we test the system, and what is deliberately simplified because this is a college project.”

Suggested duration:

1. Business and architecture overview — 15 minutes
2. Database and domain model — 15 minutes
3. Request and batch call sequences — 35 minutes
4. Security, events, reports and deployment — 15 minutes
5. Live demonstration and questions — 10 minutes

---

## 1. Start with the business problem

> “A fixed deposit, or FD, is a contract in which a customer deposits a fixed principal for a fixed tenure at an agreed interest rate. The bank owes that money to the customer, so from the bank’s accounting perspective the FD is a liability. Interest becomes an expense for the bank and an amount payable to the customer.
>
> Our application manages the complete lifecycle: defining FD products, simulating returns, opening an FD, accruing interest every day, capitalizing or paying that interest on scheduled dates, generating statements, processing premature closure, processing maturity, optionally renewing the deposit, reporting the portfolio, and notifying the customer.
>
> The most important rule is that three concepts must never be mixed:
>
> 1. Accrual means interest has been earned for a day but has not yet changed the FD balance.
> 2. Capitalization means accumulated accrued interest is added to the FD balance, after which future interest can be calculated on the larger balance.
> 3. Payout means accumulated interest is settled outside the FD instead of being added to its balance.
>
> This separation is the center of the entire design.”

Use this numerical example:

> “For ₹100,000 at 5 percent under ACTUAL/365, daily interest is ₹100,000 × 0.05 ÷ 365 = ₹13.698630. On day one, `current_balance` remains ₹100,000 and `accrued_interest` becomes ₹13.698630. On day two, the balance is still ₹100,000 and accrued interest becomes roughly ₹27.397260. If the account capitalizes quarterly, none of that increases the balance until the quarterly date. If ₹3,000 is waiting on that date, `current_balance` becomes ₹103,000, `accrued_interest` resets to zero, and subsequent daily accrual uses ₹103,000.”

Clarify the four balance-like values:

| Value | Meaning |
|---|---|
| `principal_amount` | Original contracted deposit; never overwritten |
| `current_balance` | Principal plus interest already capitalized |
| `accrued_interest` | Earned but not yet capitalized or paid |
| maturity value | `current_balance + accrued_interest` immediately before settlement |

---

## 2. Explain product rules versus an individual account

> “A product is a reusable offer from the bank. An FD account is one customer’s booked contract. The product says what is allowed; the account stores what was actually selected at opening.
>
> A product contains its currency, minimum and optional maximum deposit, minimum and maximum tenure, rate range, customer-category rate-addon cap, allowed capitalization frequencies, allowed payout frequencies, day-count convention, premature-closure permission and penalty.
>
> Allowed frequencies are normalized relational rows, not a comma-separated string. `product_compounding_options` contains one product/frequency row for MONTHLY, QUARTERLY, HALF_YEARLY or YEARLY. `product_payout_options` similarly permits those values plus MATURITY.
>
> When an account is opened, the service loads the product, verifies that it is active, checks the amount and tenure, checks that the selected currency matches the product, checks that the selected frequencies are permitted, calculates the contracted interest rate, and copies the final terms into `fd_accounts`. Copying or snapshotting matters because changing a product next month must not silently rewrite an FD that was already contracted.”

Explain compounding and payout independence:

> “Compounding frequency and payout frequency are separate. Quarterly compounding with payout at maturity means accrued interest moves into the FD balance quarterly and the complete matured balance is settled at maturity. If an interest payout becomes due before capitalization, the accrued amount is paid and reset, so a later capitalization cannot settle the same interest twice. The lifecycle always processes payout before capitalization on the same day for exactly that reason.”

Explain maturity instructions:

| Instruction | Result |
|---|---|
| `PAYOUT` | Close old FD and settle the full maturity value |
| `RENEW_PRINCIPAL` | Create a new FD for original principal and pay interest separately |
| `RENEW_PRINCIPAL_AND_INTEREST` | Create a new FD for the full maturity value |

---

## 3. Explain the overall architecture

> “This is a microservice-oriented system. We did not split every Java class into a separate service. We split deployable responsibilities where independent scaling or technology choice is useful.”

```text
Browser
  |
  v
Angular UI :4200
  |
  v
API Gateway :9090 -- validates JWT and routes requests
  |                            |
  v                            v
FD Spring Boot service :8080   Python report service :5000
  |                            |
  +------------ MySQL 8 -------+
  |
  +-- after database commit --> Kafka topic fd.lifecycle.v1
                                   |
                                   v
                         Notification service :5001
                                   |
                                   v
                             Mailpit SMTP/UI
```

> “The Angular UI owns presentation and browser interaction. The gateway is the single application-facing API entry point. The Java FD service owns transactional banking rules and the primary REST API. MySQL owns durable state. Kafka decouples business transactions from notifications. The Python notification consumer can scale or fail without rolling back an already committed FD transaction. The Python report service independently creates CSV, PDF and chart outputs. Mailpit captures demonstration email without requiring a paid provider.
>
> Docker Compose runs the complete local system. Kubernetes manifests show how each component can later have its own deployment, service, health probe and scaling policy. The API and report tiers can scale separately. MySQL and Kafka are stateful dependencies and would normally become managed services in production.”

Ports and purpose:

| Port | Component | Purpose |
|---|---|---|
| 4200 | Angular/Nginx | User interface |
| 9090 | API Gateway | Browser API entry point |
| 8080 | FD service | Swagger and direct backend debugging |
| 29092 | Kafka host listener | Local Kafka inspection |
| 8025 | Mailpit UI | View captured email |
| 1025 | Mailpit SMTP | Local email delivery |

> “MySQL, the report service and notification service are not published as normal host APIs in Compose. They are exposed inside the Docker network. That reduces accidental bypass of the gateway.”

---

## 4. Repository tour

> “At repository root, `docker-compose.yml` describes the local distributed system. `.env.example` documents configurable secrets and database settings. `k8s/` contains deployment manifests. `docs/` contains integration, event and laboratory traceability documents. `deliverables/` contains the report, presentation, demonstration video and scripts.
>
> `fd-angular-ui/` is the Angular frontend. `api-gateway/` is Spring Cloud Gateway. `fd-microservice/` is the Java banking domain service. `notification-service/` is the Kafka consumer. `report-service/` is the Python export service.
>
> Inside the FD service, controllers accept HTTP requests, DTOs define request and response contracts, services contain business rules, repositories perform persistence, entities map tables, schedulers start background jobs, event classes describe lifecycle facts, helpers contain reusable financial/date rules, and Flyway migrations version the schema.”

Explain the Java layering rule:

```text
HTTP request
  -> Controller: transport, validation and authorization boundary
  -> Service: transaction and business decisions
  -> Helper/other service: focused calculation or lifecycle operation
  -> Repository: database access
  -> Entity/table: durable state
  -> EventPublisher: announce committed lifecycle fact
```

> “Controllers should not calculate interest. Repositories should not make business decisions. Entities represent state but do not orchestrate workflows. Services are where the use cases live.”

---

## 5. Database model, table by table

### `users` and `customer_profile`

> “`users` contains login identity, BCrypt password hash, email and role. `customer_profile` maps a login user to a business customer ID such as `CUST001`. Admin and officer users normally have no customer profile. This distinction is why username and customer ID must not be confused.”

### `products`

> “`products` is the source of permitted financial rules. Product option tables normalize the allowed compounding and payout choices. Existing FDs retain their own booked rate and selections even if the product changes.”

### `fd_accounts`

> “`fd_accounts` is the aggregate root. Its primary key is `fd_account_no`. It stores customer and product references, currency, immutable original principal, current balance, booked interest rate, tenure, both frequencies, status, start and maturity dates, unsettled interest, schedule cursors, maturity instruction, processing marker and renewal link.”

Statuses:

| Status | Meaning |
|---|---|
| `ACTIVE` | Accruing and eligible for scheduled processing |
| `CLOSED` | Matured and paid out |
| `PREMATURE_CLOSED` | Closed before maturity under product rules |
| `RENEWED` | Old FD matured into a new FD |

### `fd_interest_transactions`

> “This is the daily accrual sub-ledger. The unique account/date pair prevents two accrual records for one account on one business date. It stores the day’s amount, cumulative unsettled interest at that point, and settlement state: PENDING, CAPITALIZED, PAID or PREMATURE_CLOSURE.”

### `fd_transactions`

> “This is the append-only financial event ledger. Each row has a precise transaction type, amount, currency, debit GL, credit GL, execution timestamp, effective business date, globally unique UUID and stable unique reference ID. Application code never edits previous financial events.”

Transaction meanings and simplified GL mappings:

| Event | Debit | Credit |
|---|---|---|
| `DEPOSIT` | Customer remittance asset | FD deposit liability |
| `INTEREST_ACCRUAL` | Interest expense | Accrued-interest liability |
| `INTEREST_CAPITALIZATION` | Accrued-interest liability | FD deposit liability |
| `INTEREST_PAYOUT` | Accrued liability or maturity clearing | Customer settlement asset |
| `FD_MATURITY` payout | FD deposit liability | Customer savings/settlement |
| `FD_MATURITY` renewal | FD deposit liability | Maturity clearing |
| `FD_RENEWAL` | Maturity clearing | New FD deposit liability |
| `PREMATURE_CLOSURE` | FD deposit liability | Customer savings |
| `PENALTY` | FD deposit liability | Premature-closure income |

> “These are simplified student-project accounts, not an entire bank chart of accounts, but debit and credit meaning stays internally consistent.”

### `fd_statements`

> “A statement records opening balance, interest accrued during the statement date, interest capitalized, interest paid, closing balance and accrued interest still outstanding. Therefore a user can see earned interest without falsely showing it in principal balance.”

### Supporting tables

> “`fd_account_sequence` stores a branch counter and is locked during account-number generation. `notification_log` provides a delivery audit and stores Kafka event IDs so notifications are idempotent.”

### Schema ownership

> “Flyway owns schema changes. The service uses `ddl-auto=validate`, meaning Hibernate checks that entity mappings match the migrated schema but does not invent or mutate production tables. Migrations V1 through V15 create the original model and supporting features; V16 separates the realistic financial lifecycle and corrects legacy interest-credit data; V17 adds maximum deposit and product range constraints.”

---

## 6. Authentication and authorization call sequence

Say this while logging in:

> “The Angular login component calls `AuthService.login()`, which posts credentials to `/api/auth/login`. Authentication routes bypass the gateway JWT filter because no token exists yet. `AuthController.login()` delegates to `AuthServiceImpl.login()`.
>
> `AuthServiceImpl` creates a `UsernamePasswordAuthenticationToken` and asks Spring’s `AuthenticationManager` to authenticate it. The DAO authentication provider calls `CustomUserDetailsService`, loads the user from `UserRepository`, and compares the supplied password with the BCrypt hash using the configured password encoder.
>
> After successful authentication, the service loads the optional `CustomerProfile`, obtains `customerId`, determines the role, and calls `JwtTokenProvider.generateToken()`. The signed token contains subject=username, role, issued time, expiry time and customerId when the login belongs to a customer.
>
> Angular stores the token in local storage. `jwtInterceptor` automatically adds `Authorization: Bearer <token>` to later non-auth requests. `authGuard` blocks logged-out routes, and `roleGuard` hides role-specific pages. These browser guards are for usability; they are not the security boundary.
>
> At the gateway, `JwtValidationFilter` verifies the HMAC signature and expiration. It removes any caller-supplied identity headers, then creates trusted `X-User-Id`, `X-User-Role` and optional `X-Customer-Id` headers. The Java service independently validates the JWT again in `JwtAuthenticationFilter`, creates a Spring `Authentication`, and places customerId in its credentials field. Method-level `@PreAuthorize` rules enforce roles. Therefore changing Angular code cannot grant permission.”

Role matrix:

| Operation | Customer | Officer | Admin |
|---|---:|---:|---:|
| Calculate FD | Yes | No current endpoint permission | No current endpoint permission |
| View own accounts | Yes | — | — |
| View all accounts | No | Yes | Yes |
| Open FD | No | Yes | Yes |
| Premature closure | Own FD | Yes | No current endpoint permission |
| Product administration | No | No | Yes |
| Batch/time travel | No | No | Yes |
| Summary reporting | No | Yes | Yes |

> “For account detail, transaction and statement endpoints, `FdAccountController.assertAccountAccess()` allows officers/admins through, but when the role is CUSTOMER it compares token customerId with the account’s stored customerId. This prevents one customer from guessing another account number.”

Demo credentials all use `admin123`: `admin` (configuration/batch), `officer1` (maker), `checker1` (independent authorizer), `auditor1` (read-only audit), and `johndoe` (customer `CUST001`).

---

## 7. Product creation and validation sequence

> “An admin request enters `ProductController.createProduct()`, then `ProductServiceImpl.createProduct()`. The service rejects duplicate product codes, invalid min/max ranges, unsupported day-count conventions and a default compounding frequency that is not included in the product’s allowed set. Frequencies are normalized to uppercase, and legacy `HALFYEARLY` becomes `HALF_YEARLY`. The product and element-collection option rows are saved in one transaction.
>
> At FD opening, `ProductServiceImpl.validateProductForFd()` checks active status, tenure range, minimum deposit and optional maximum deposit. `FdBusinessRules.requireCompounding()` and `requirePayout()` perform the account-choice validation against the normalized option sets.”

> “Only ACTUAL/365 is currently accepted. Making the convention a product field keeps the model extensible, but rejecting unimplemented conventions is safer than silently calculating them incorrectly.”

---

## 8. Calculator sequence

> “The calculator is a quotation tool, not a booking operation. `FdCalculatorComponent.calculate()` calls `FdCalculatorService.simulate()`, which posts to `/api/fd/calculator/simulate`. `FdCalculatorController.simulate()` calls `FdCalculatorServiceImpl.calculate()`.
>
> The service applies category addons: senior citizen adds 0.50 percentage points, staff adds 1.00, with the total addon capped at 2.00 for this calculator path. For SIMPLE calculation it uses principal × rate × months/12. For compound calculation it maps monthly, quarterly, half-yearly or yearly to 12, 4, 2 or 1 periods per year and computes the projected amount. It returns a simulation only; it does not create an account or ledger event.
>
> `/api/fd/calculate` is a compatibility endpoint for the lab contract and calls the same service.”

Clarify the difference:

> “The calculator’s compound formula is a customer preview. The live account lifecycle does actual daily accrual followed by calendar settlement. The live ledger is authoritative.”

---

## 9. FD opening — exact function sequence

Use this call chain:

```text
FdCreateComponent.ngOnInit
  -> loadProducts
  -> ProductService.getProducts

User selects product
  -> onProductChange
  -> UI displays product-permitted options

User submits
  -> FdCreateComponent.onSubmit
  -> FdAccountService.createAccount
  -> POST /api/fd/account/create
  -> Gateway JwtValidationFilter
  -> backend JwtAuthenticationFilter
  -> FdAccountController.createAccount
  -> FdAccountServiceImpl.createAccount
```

Then narrate service internals:

> “`createAccount()` first calls `productService.validateProductForFd()`. It normalizes and validates currency through `CurrencyRules`; supported currencies are INR, USD, EUR, GBP, JPY, AED and KWD, with their correct zero-, two- or three-decimal minor units. Product currency and requested currency must match.
>
> It gets the product’s allowed sets, validates the selected compounding frequency and payout frequency, validates the maturity instruction, chooses the explicit start date or today, and calculates maturity with `startDate.plusMonths(termMonths)`. We use calendar months, not 30-day approximations.
>
> It calls `InterestCalculationHelper.applyCategoryAddons()` and caps the final rate at the product maximum. This final rate is the account’s contracted snapshot.
>
> It calls `AccountNumberGenerator.generate(branchCode)`. That method locks the branch sequence row using `findByBranchCodeForUpdate()`, increments the six-digit sequence, concatenates three-digit branch plus sequence, calculates digit-sum modulo 10 as a checksum, and returns the ten-digit number. Locking prevents two concurrent requests receiving the same sequence.
>
> The service creates `FdAccount`: principal and current balance begin equal; accrued interest is zero; status is ACTIVE; last accrual date is one day before start so start date can be accrued; next capitalization and payout dates come from real `plusMonths` calendar arithmetic; and a UUID is assigned.
>
> After saving the account, `FdTransactionService.recordDeposit()` creates the initial ledger row. Its reference `DEPOSIT:<account>` prevents a duplicate deposit event. Finally `EventPublisher.publishFdOpened()` announces the event.
>
> The whole service method is transactional. If account saving or deposit recording fails, both roll back. In Kafka mode, the bridge only publishes after the database commit, preventing a notification about a rolled-back account.”

Opening accounting:

```text
Dr ASSET_CUSTOMER_REMITTANCE
Cr LIABILITY_FD_DEPOSITS
```

---

## 10. Reading accounts, transactions and statements

> “`FdListComponent.loadAccounts()` checks the logged-in role. Customers call `FdAccountService.getMyAccounts()` and the backend extracts customerId from the JWT; officers and admins call `getAllAccounts()`.
>
> `FdDetailComponent.loadData()` calls `GET /api/fd/account/{id}`, while `loadTransactions()` calls the transaction endpoint. Statement viewer calls the statements endpoint. After ownership checks, `FdAccountServiceImpl` delegates to the appropriate repository and `FdAccountMapper` constructs response DTOs.
>
> The UI intentionally displays original principal, current balance and accrued interest separately.”

---

## 11. Daily accrual — exact business and function sequence

```text
01:00 scheduler
  -> DailyInterestAccrualJob.executeDailyAccrual
  -> processInterestAccrual(today)
  -> FdAccountRepository.findAllActiveAccounts
  -> for each account: InterestLifecycleService.processAccountThroughDate
  -> InterestLifecycleServiceImpl.processAccountThroughDate
  -> lock account with findByIdForUpdate
  -> for each missing business date:
       accrueOneDay
       processDuePayout
       processDueCapitalization
```

> “`processAccountThroughDate()` caps its end date at maturity. It begins at `last_accrual_date + 1`, or at start date if no cursor exists. This lets one run catch up multiple missed days after downtime.
>
> `accrueOneDay()` first queries for an existing account/date row. If present, it only advances the cursor and does not calculate again. Otherwise `InterestEngineServiceImpl.calculateDailyAccrualForAccount()` chooses `current_balance`, then `InterestCalculationHelper.calculateDailyAccrual()` calculates balance × annualRate/100 ÷ 365 using BigDecimal. It rounds accrual to six decimals.
>
> The method creates a PENDING `FdInterestTransaction`, adds the amount to `fd_accounts.accrued_interest`, updates `last_accrual_date`, records an `INTEREST_ACCRUAL` transaction and publishes an `InterestAccruedEvent`.
>
> Current balance is not touched. That is the key invariant.”

Accrual accounting:

```text
Dr EXPENSE_INTEREST
Cr LIABILITY_ACCRUED_INTEREST
```

Idempotency protections:

- `last_accrual_date` avoids re-walking processed dates.
- Unique `(fd_account_no, accrual_date)` prevents duplicate daily records.
- Reference `ACCRUAL:<account>:<date>` prevents duplicate ledger rows.
- Pessimistic account lock prevents concurrent lifecycle threads updating the same FD simultaneously.

---

## 12. Scheduled payout sequence

> “After each day’s accrual, `processDuePayout()` checks whether payout frequency is not MATURITY and whether `businessDate` has reached `next_payout_date`. It advances the last and next payout cursors using `FdBusinessRules.nextScheduledDate()`.
>
> If accrued interest is positive, it records `INTEREST_PAYOUT`, marks all pending accrual rows through that date as PAID, resets account accrued interest to zero and publishes `InterestPaidEvent`. Current FD balance does not increase.”

Accounting:

```text
Dr LIABILITY_ACCRUED_INTEREST
Cr ASSET_CUSTOMER_SETTLEMENT
```

> “The project simulates the receiving savings/payment account using GL entries. A production integration would call a payment or core savings service with an idempotency key.”

---

## 13. Capitalization sequence

> “`processDueCapitalization()` checks `next_capitalization_date`. On a due date it advances the schedule cursor. If accrued interest is positive, it adds that amount to `current_balance`, rounds to the account currency, resets accrued interest, marks pending daily rows CAPITALIZED, records `INTEREST_CAPITALIZATION` and publishes `InterestCapitalizedEvent`.
>
> Future daily accrual now uses the larger current balance. This is where compounding actually happens.”

Accounting:

```text
Dr LIABILITY_ACCRUED_INTEREST
Cr LIABILITY_FD_DEPOSITS
```

Idempotency:

- Schedule cursor moves forward.
- Previously settled daily rows are no longer PENDING.
- Reference `CAPITALIZATION:<account>:<date>` is unique.

---

## 14. Statement generation sequence

```text
02:00 scheduler
  -> StatementGenerationJob.executeStatementGeneration
  -> generateStatements(today)
  -> find active accounts
  -> skip existing account/date statement
  -> sum daily accrual for date
  -> sum capitalization ledger amount for date
  -> sum payout ledger amount for date
  -> save FdStatement
```

> “Closing balance comes from `current_balance`. Opening balance is closing minus capitalization for that date. Accrued interest is separately captured from the account. The unique account/date constraint plus repository lookup makes statement creation repeat-safe.”

---

## 15. Maturity — exact function sequence

```text
04:00 scheduler
  -> MaturityProcessingJob.executeMaturityProcessing
  -> MaturityService.processMaturedAccounts(today)
  -> repository finds ACTIVE accounts with maturity_date <= today
  -> for each: processOne(account, today)
       -> lifecycleService.processAccountThroughDate
       -> lock account
       -> reject if no longer ACTIVE or already marked processed
       -> calculate maturity amount
       -> record FD_MATURITY
       -> execute maturity instruction
       -> set maturity_processed_at
       -> save final statement and publish FD_MATURED
```

> “The lifecycle is caught up through maturity before settlement. `calculateMaturityAmount()` returns current balance plus any unsettled accrued interest, rounded by currency.”

### PAYOUT

> “The maturity ledger debits FD deposit liability and credits customer savings. The account becomes CLOSED, and balance and accrued interest become zero.”

### RENEW_PRINCIPAL

> “`FD_MATURITY` moves the old liability to `MATURITY_CLEARING`. Interest is paid from clearing. `createRenewal()` generates a new account for original principal, copies the booked terms, creates new schedule dates, and sets it ACTIVE. The old account becomes RENEWED and stores the new account number. `FD_RENEWAL` moves the principal from clearing into the new FD liability.”

### RENEW_PRINCIPAL_AND_INTEREST

> “The same renewal flow creates the new account for the entire maturity amount; no separate interest payout is required.”

Idempotency:

- Query only finds ACTIVE matured accounts.
- Row is locked.
- `maturity_processed_at` is a second explicit guard.
- Old status changes to CLOSED or RENEWED.
- Stable `FD_MATURITY:<account>` and `FD_RENEWAL:<account>` references are unique.

> “Running the maturity job twice therefore cannot create a second payout or second renewal.”

---

## 16. Premature closure — exact function sequence

```text
FdWithdrawComponent.onSubmit
  -> FdAccountService.withdraw
  -> POST /api/fd/account/withdraw
  -> FdAccountController.withdraw
  -> assertAccountAccess
  -> WithdrawalServiceImpl.processWithdrawal
```

> “The service verifies ACTIVE status, ensures withdrawal is not before start and is strictly before maturity, loads the product and checks `premature_closure_allowed`.
>
> It catches the lifecycle up through the withdrawal date, locks the account, then calculates gross value as current balance plus pending accrued interest. Gross interest is gross value minus original principal. Penalty equals gross interest × product penalty percentage. Net payout equals original principal plus gross interest minus penalty.
>
> Pending accrual records become PREMATURE_CLOSURE. The account becomes PREMATURE_CLOSED and its stored balance/accrual become zero. The service records separate PREMATURE_CLOSURE and PENALTY ledger entries, creates a final statement, publishes the closure event and returns principal, interest, penalty and net payout.”

> “We do not use a naive annual-rate-times-months calculation. We use the actual accrued and capitalized state up to the closure date, then apply the product rule.”

Repeat protection comes from ACTIVE-status validation and unique closure/penalty references.

---

## 17. Transaction service and idempotency design

> “Every ledger operation goes through `FdTransactionServiceImpl.record()`. Each public method chooses the exact business type, GL accounts, remarks and deterministic reference ID. `record()` first checks `existsByReferenceId()`. If the reference exists it returns the prior transaction; otherwise it inserts a new row with UUID, business date and execution timestamp.
>
> Business date answers ‘which banking day does this affect?’ Transaction timestamp answers ‘when did software execute it?’ They can differ during catch-up or time travel.”

Explain the layered idempotency model:

1. State cursor: last accrual/capitalization/payout date.
2. Status/marker: ACTIVE and maturity processed marker.
3. Detail uniqueness: one interest row and statement per account/date.
4. Ledger reference uniqueness: deterministic business-event reference.
5. Database transaction and row lock: concurrency protection.

> “Banking safety should not depend on one `if` statement. These layers protect against retries, scheduler overlap and accidental duplicate API calls.”

---

## 18. Kafka and notification sequence

```text
Business service publishes Spring domain event
  -> database transaction commits
  -> KafkaLifecycleEventBridge @TransactionalEventListener(AFTER_COMMIT)
  -> serialize version 1.0 envelope
  -> Kafka topic fd.lifecycle.v1, key = fdAccountNo
  -> Python notification consumer group
  -> check notification_log by eventId
  -> send email to Mailpit
  -> insert delivery audit
  -> commit Kafka offset
```

> “The event envelope contains schemaVersion, UUID eventId, occurredAt, eventType, customerId, account number, currency, amount, subject and message. Account number is the Kafka key, so events for one FD maintain partition order.
>
> The consumer disables automatic offset commits. Before sending, it asks whether eventId already exists in `notification_log`. After successful email and audit insert, it commits the Kafka offset. A redelivered event whose audit row already exists is recognized and not emailed twice.
>
> Event types are FD_OPENED, INTEREST_ACCRUED, INTEREST_CAPITALIZED, INTEREST_PAID, FD_MATURED, FD_RENEWED and FD_PREMATURELY_CLOSED.
>
> When Kafka mode is disabled, `EventListenerHandler` handles the same Spring events asynchronously inside the Java service and `NotificationServiceImpl` uses JavaMail. Conditional configuration ensures we use either the direct listener or Kafka bridge, not both.”

Why Kafka:

> “Notifications are not allowed to slow or couple the core banking transaction. Other teams can add independent Kafka consumer groups for analytics, audit or fraud monitoring without changing FD code.”

Honest limitation:

> “We publish after commit but do not yet use a transactional outbox. If the database commit succeeds and Kafka is unavailable at the exact publish moment, the bridge logs the failure but does not durably retry from an outbox. A production upgrade should add an outbox table and relay.”

> “The demonstration consumer records a failed email as FAILED. Because deduplication currently treats any existing eventId as processed, failed delivery is audited but is not automatically retried. Production should use explicit retry state, attempt counters and a dead-letter topic. There is also a partial-failure window if SMTP accepts an email and the subsequent audit insert fails; true exactly-once email delivery is not claimed.”

---

## 19. Reporting flows

### Java reports

> “`ReportController` delegates to `ReportServiceImpl`. Summary reporting loads products and accounts, groups accounts by product and calculates account counts, status counts, total original principal and unsettled accrued interest. Customer portfolio takes customerId from the trusted JWT, never from an unrestricted query parameter. CSV export serializes the summary.”

### Python report service

> “Gateway routes `/reports/**` to the Python service after validating JWT and setting trusted role/customer headers. The service directly queries MySQL for read-only aggregation and can return CSV, PDF or a PNG customer portfolio chart. Customers may only chart their own token customerId; officers/admins may specify a customer.”

> “Sharing the operational database is acceptable for this demonstration. At scale, reporting should use a read replica, warehouse or event-built read model to avoid analytic load on the transactional database.”

---

## 20. Admin jobs and time travel

> “Normal schedules run accrual at 01:00, statements at 02:00 and maturity at 04:00. Admin endpoints can trigger today’s jobs manually.
>
> For a college demonstration we cannot wait months, so `POST /api/admin/time-travel` accepts a target date and operation. `AdminController.timeTravel()` calls `TimeTravelServiceImpl.executeTimeTravel()`, which dispatches to accrual, maturity, statements or ALL using the target date. It does not change the operating-system clock. It simply executes business-date-aware methods.
>
> Because all jobs are idempotent, repeating the same time-travel operation should not double-credit money.”

---

## 21. Frontend structure and behavior

> “Angular uses lazy standalone route components. `app.routes.ts` maps login, dashboard, calculator, account creation/list/detail/withdrawal, statements, admin batches/time travel and reports. `app.config.ts` installs router, HTTP client with JWT interceptor, and English/Hindi translation loading.
>
> Core services are thin HTTP clients: AuthService, ProductService, FdAccountService, FdCalculatorService, AdminService and ReportService. Feature components own screen state and call those services. The models file mirrors backend DTO shapes. `currency-format.pipe` displays the correct currency formatting.
>
> Opening UI first fetches products, then changes the frequency dropdowns to options permitted by the selected product. These dropdowns improve UX, while backend validation remains authoritative.”

Important component sequences:

- Login: `LoginComponent.onSubmit()` → `AuthService.login()` → token saved → dashboard.
- Dashboard: `ngOnInit()` → role-specific data load.
- Submit FD: `ngOnInit()` → `loadProducts()` → `onProductChange()` → `onSubmit()` → `POST /api/fd/opening-requests`. The CHECKER queue calls approve/reject; approval invokes the existing atomic opening service and only then creates the FD/deposit/outbox record.
- Account list: `ngOnInit()` → `loadAccounts()` → role chooses own/all API.
- Detail: route account number → `loadData()` and `loadTransactions()`.
- Statements: route account number → `loadStatements()`.
- Withdrawal: load account → confirmation → `onSubmit()`.
- Batch control: `runJob()` chooses the AdminService endpoint.
- Time travel: `executeTimeTravel()` posts operation and target date.
- Reports: `loadData()` chooses portfolio or summary by role; `exportCsv()` downloads a Blob.

---

## 22. Error handling and validation

> “DTO annotations reject missing or invalid request fields before service execution. Services throw domain exceptions such as `FdNotFoundException`, `ProductNotFoundException` and `InvalidOperationException`. `GlobalExceptionHandler` converts these into consistent API error responses.
>
> Database constraints are the final safety layer: positive principal and tenure, nonnegative rate and balance, valid status/frequency/instruction values, three-letter currency, maturity after start, product min/max ranges, foreign keys, and unique business references.”

> “We use `BigDecimal` in Java and DECIMAL in MySQL. Binary floating point is never used for stored money calculations. Daily accrual retains six decimals; account balances retain the existing three-decimal storage scale so KWD is supported; display and settlement use currency-specific minor units.”

---

## 23. Startup and deployment sequence

Say while running the stack:

```powershell
docker compose up --build -d
docker compose ps
```

> “Compose starts MySQL, Kafka and Mailpit, waits for health conditions, then starts the FD service, reporting, notification consumer, gateway and UI. The FD service runs Flyway before accepting traffic, then Hibernate validates mappings. Health endpoints let Compose and Kubernetes decide readiness.
>
> Configuration comes from environment variables: database URL/credentials, JWT secret, Kafka bootstrap servers/topic and SMTP settings. Local defaults are demonstration-only. `.env`, real secrets and Kubernetes `secret.yaml` must not be committed.”

Useful URLs:

- UI: `http://localhost:4200`
- Gateway: `http://localhost:9090`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Mailpit: `http://localhost:8025`

Stop without deleting data:

```powershell
docker compose down
```

Reset deterministic demo data only when intentional:

```powershell
docker compose down -v
docker compose up --build -d
```

> “The `-v` matters because it deletes the MySQL Docker volume. Never use it casually.”

---

## 24. Testing strategy and evidence

> “The backend currently has 34 passing automated tests with zero failures. Tests cover account-number locking/checksum, interest calculations, calendar schedules, account opening, products and limits, daily accrual separation, duplicate accrual protection, capitalization/reset, statements, maturity idempotency, withdrawals and reports. The Spring integration test loads the application with H2 for endpoint and security coverage.
>
> Angular is verified with a production build. Docker validation applies real Flyway migrations to MySQL and runs end-to-end scenarios.”

Commands:

```powershell
cd fd-microservice
mvn test

cd ..\fd-angular-ui
npm ci
npm run build
```

End-to-end scenarios already verified:

- Invalid `WEEKLY` compounding is rejected and creates no account.
- Repeating accrual for the same date creates no duplicate.
- A full-year simulation produced 12 monthly, 4 quarterly, 2 half-yearly and 1 yearly capitalization.
- Accrued interest remains outside current balance until settlement.
- Statements separate accrued, capitalized, paid and outstanding interest.
- All maturity instructions work.
- Repeating maturity creates no second payout or renewal.
- Premature closure applies product penalty; repeating closure is rejected.
- Kafka events, notifications and GL mappings persist correctly.

---

## 25. Integration with other teams

> “The module is standalone for demonstration but uses stable integration keys: customerId, productCode and fdAccountNo. It preserves compatibility endpoints from the lab manual.
>
> In the combined team system, the central gateway should remain the browser entry point. Authentication may move to the authentication team, product rules to the product/pricing team, customer data to the customer service, and payout execution to savings/payments. JWT should pass unchanged. Kafka is the preferred asynchronous boundary.
>
> Before final integration, teams must agree on calculation lookup by calcId, canonical customer claim, complete product payload, final `/api/v1` routing, event ownership, and payout API/idempotency semantics. These open external contracts are documented in `docs/INTEGRATION.md`; they are not missing internal FD logic.”

---

## 26. Deliberate simplifications and production upgrades

Be explicit rather than pretending this is a complete commercial bank:

1. GL accounts are illustrative; there is no enterprise general-ledger service.
2. Customer settlement is represented by ledger entries; no real savings/payment transfer occurs.
3. Kafka publishing should gain a transactional outbox for guaranteed delivery.
4. Daily accrual customer email can be noisy; production would support notification preferences or digesting.
5. Registration currently accepts a requested role for lab convenience; production role assignment must be administrator-controlled.
6. Gateway `RateLimiterConfig` is logging-only; production needs Redis/token-bucket enforcement.
7. Report service currently reads the same database; production should use a replica/read model.
8. JWT local default and demo credentials must be replaced before any real deployment.
9. Kafka is single-node and MySQL single-instance locally; production requires replicated managed infrastructure, backups and disaster recovery.
10. FD opening now has an in-module maker-checker control and operational audit log. External KYC/AML, bank-wide workflow/audit, tax deduction, lien, nomination, holiday calendar and regulatory reporting remain beyond the student-project boundary and must integrate through APIs/events rather than database joins.
11. Notification delivery is idempotent for recorded event IDs, but failed delivery needs a proper retry/dead-letter policy before production use.

> “These are not hidden defects. They mark the boundary between a realistic teaching implementation and a full regulated core-banking platform.”

---

## 27. Complete live demonstration script

1. **Show architecture**

   > “The browser only calls the gateway. Banking state changes happen in the FD service. Kafka makes notifications asynchronous.”

2. **Log in as customer `johndoe`**

   > “The returned JWT contains username, CUSTOMER role and CUST001. The UI can display only this customer’s FDs.”

3. **Run calculator**

   > “This is a quote, not an account. No financial rows are created.”

4. **Log in as `officer1` and open an FD**

   > “The product controls legal choices. The account snapshots selected terms. Account and deposit ledger insert atomically.”

5. **Open account detail and transaction history**

   > “Original principal, current balance and accrued interest are separate. The first ledger event is DEPOSIT.”

6. **Open Mailpit**

   > “The committed opening event travelled through Kafka to an independent notification consumer.”

7. **Log in as `admin`; run accrual time travel**

   > “Each missing date creates one daily accrual. Current balance does not move before settlement.”

8. **Travel to a capitalization date**

   > “Accumulated accrual moves into current balance once and resets.”

9. **Generate/open statement**

   > “The statement distinguishes the day’s accrual, capitalization, payout and remaining accrual.”

10. **Demonstrate maturity or premature closure**

    > “Maturity follows the selected instruction; premature closure follows the product penalty rule. Re-running the same process cannot duplicate money.”

11. **Show reports and containers**

    > “Reports scale independently, and every deployable unit has a health check.”

---

## 28. Rapid-fire questions and exact answers

**Why not add daily interest directly to balance?**  
Because earning interest and capitalizing it are different contractual events. Adding it daily would falsely create daily compounding.

**Why preserve principal?**  
Original principal is a contract and audit fact. Current balance is the mutable compounded value.

**Why store both a daily interest table and transaction ledger?**  
The interest table explains per-day calculation and settlement state. The transaction ledger records accounting events and GL impact.

**Why both last-date fields and unique constraints?**  
Cursors make processing efficient; constraints protect correctness even under races or defects.

**Why use calendar months?**  
Quarterly means three calendar months, not always 90 days.

**Why ACTUAL/365 in leap years?**  
It is the explicitly selected student-project convention: actual elapsed days with a fixed denominator of 365.

**Why does capitalization use current balance for later accrual?**  
That is compounding: capitalized interest becomes part of the interest-bearing balance.

**Why use BigDecimal?**  
Binary floating point cannot exactly represent many decimal currency amounts.

**Why Kafka instead of calling email synchronously?**  
The money transaction must not wait for or depend on email, and other consumers can subscribe independently.

**What happens if a job is missed?**  
The next run starts from last accrual date plus one and catches up each missing date through the requested date or maturity.

**What happens if two jobs run together?**  
The account row lock serializes lifecycle processing; unique account/date and reference constraints prevent duplicates.

**Can an active FD’s terms be edited?**  
There is no normal account-update API. Product changes affect future bookings, not existing contracted snapshots.

**Is accrued interest included in current balance?**  
No. Only capitalization changes current balance.

**Is this production ready?**  
It demonstrates realistic domain separation, security, idempotency, migrations, events and scaling boundaries. A regulated deployment still needs the production upgrades listed above.

---

## 29. Final summary to say verbatim

> “The project is built around one invariant: every financial event has one precise meaning. Opening creates principal and a deposit liability. Daily accrual recognizes expense and accrued liability without changing FD balance. Capitalization moves accrued liability into FD liability. Payout settles accrued liability externally. Maturity closes or renews exactly once according to instruction. Premature closure uses actual lifecycle state and product penalty rules.
>
> Technically, Angular calls a JWT-protected gateway; controllers delegate to transactional services; services validate product rules, lock aggregate rows and write normalized MySQL records; deterministic references and database constraints make operations idempotent; post-commit events go through Kafka; independent Python services deliver notifications and reports; Docker and Kubernetes keep components deployable and scalable.
>
> If you remember the separation between product and account, principal and current balance, accrual and capitalization, business state and append-only ledger, and synchronous money processing versus asynchronous notifications, you understand the architecture of this project.”
