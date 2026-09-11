# Fixed Deposit (FD) Microservice

A production-grade, domain-driven Core Banking Microservice for managing the complete lifecycle of Fixed Deposits. Built with **Spring Boot 3.2.4** and **Java 17**.

---

## 🏛️ Architecture & Key Highlights

- **Insert-Only Financial Ledger:** Immutable ledger entries for deposits, interest accruals, premature withdrawals, penalties, and maturity payouts.
- **Dynamic Interest Engine:** Calculates Simple & Compound interest across Monthly, Quarterly, Half-Yearly, and Yearly compounding frequencies with product-capped category add-ons (Senior Citizen, Staff).
- **Stateless Role-Based Security:** JWT authentication with embedded `customerId` claims for fast, zero-lookup customer authorization.
- **Asynchronous Lifecycle Events:** Non-blocking event listeners handle notifications strictly on:
  1. `FD_OPENED` (Account creation)
  2. `FD_MATURED` (Automatic maturity payout)
  3. `FD_WITHDRAWN` (Premature withdrawal & closure)
- **Automated Schedulers:** Nightly interest accruals, maturity payouts, and monthly statement generation jobs.

---

## 🚀 Quick Start & Running Locally

### Option 1: H2 In-Memory Dev Mode (Zero setup required)
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```
- **Swagger UI / OpenAPI Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 Web Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:fddb`, Username: `sa`, Password: *(leave blank)*)

### Option 2: MySQL Production Mode
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=your-256-bit-secret-key-for-hmac-sha256

mvn spring-boot:run
```

---

## 🔑 Default Seed Test Accounts (Flyway Migrations V10 & V11)

| Username | Password | Role | Description |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Full administrative, product creation & time-travel access |
| `officer1` | `admin123` | `ROLE_BANK_OFFICER` | Bank branch officer for account creation & reporting |
| `johndoe` | `admin123` | `ROLE_CUSTOMER` | Customer mapped to Customer ID `CUST001` |

---

## 📡 API Reference & Integration Endpoints

All secured endpoints require the HTTP header:
`Authorization: Bearer <jwt_token>`

### 1. Authentication (`/api/auth`)
- `POST /api/auth/register` — Register a new user (`CUSTOMER`, `BANK_OFFICER`, `ADMIN`)
- `POST /api/auth/login` — Authenticate and receive JWT + `customerId`

### 2. Public Simulation (`/api/fd/calculator`)
- `POST /api/fd/calculator/simulate` — **Public (No Auth)**. Simulate maturity amounts, interest earned, and category rate add-ons before booking.

### 3. FD Account Management (`/api/fd`)
- `POST /api/fd/account/create` (`BANK_OFFICER`, `ADMIN`) — Opens an FD account and books the initial deposit ledger record.
- `GET /api/fd/account/{fdAccountNo}` — Retrieves detailed account status and accrued interest.
- `GET /api/fd/accounts/my` (`CUSTOMER`) — Returns all FD accounts belonging to the authenticated customer.
- `GET /api/fd/accounts/all` (`BANK_OFFICER`, `ADMIN`) — Lists all bank FD accounts.
- `POST /api/fd/account/manual-close` (`BANK_OFFICER`, `ADMIN`) — Closes a matured FD (rejects premature accounts).

### 4. Financial Ledger & Transactions (`/api/fd`)
- `GET /api/fd/account/{fdAccountNo}/transactions` — Full double-entry transaction history (`DEPOSIT`, `WITHDRAWAL`, `PENALTY`, `MATURITY_PAYOUT`).
- `GET /api/fd/account/{fdAccountNo}/statements` — Monthly statement history.
- `POST /api/fd/account/withdraw` (`CUSTOMER`, `BANK_OFFICER`) — Prematurely closes an FD, applies penalty, and creates separate payout & penalty ledger entries.

### 5. Product Management (`/api/product`)
- `POST /api/product` (`ADMIN`) — Create new deposit product definitions (rates, tenures, penalty %).
- `GET /api/product/{code}` — Get product details.
- `GET /api/product/all` — List active products.

### 6. Reports & Analytics (`/api/report`)
- `GET /api/report/fd-summary` (`BANK_OFFICER`, `ADMIN`) — Product-wise portfolio metrics.
- `GET /api/report/customer-portfolio` (`CUSTOMER`) — Customer portfolio report.
- `GET /api/report/export/csv` (`ADMIN`) — Download CSV summary.

### 7. Time-Travel Simulation (`/api/admin`)
- `POST /api/admin/time-travel` (`ADMIN`) — Fast-forward time for testing accruals, statements, and maturity.

---

## 🧪 Running Automated Tests

```bash
mvn clean test
```
- **Total Automated Tests:** 27
- **Test Pass Rate:** 100% (Unit, Repository, Service, and MockMvc E2E integration tests)

---

## 🤝 Integration Guide for Other Banking Modules

Other modules can integrate with this FD microservice as follows:

1. **Savings / Current Accounts Module:**
   - On FD creation (`POST /api/fd/account/create`), debit the customer's savings account (`ASSET_CUSTOMER_REMITTANCE`).
   - On premature withdrawal or maturity payout (`POST /api/fd/account/withdraw` or automatic maturity), credit the customer's savings account (`ASSET_CUSTOMER_SAVINGS`).
2. **Customer Onboarding / Profile Module:**
   - Provide `customerId` and category eligibility (`SENIOR_CITIZEN`, `STAFF`, `GENERAL`).
3. **General Ledger (GL) Accounting Module:**
   - Reconcile via `/api/fd/account/{no}/transactions` using standardized GL accounts:
     - `LIABILITY_FD_DEPOSITS`
     - `EXPENSE_INTEREST_PAID`
     - `INCOME_PREMATURE_PENALTY`
     - `ASSET_CUSTOMER_SAVINGS`
4. **Notification Gateway Module:**
   - Consume Spring Application Events or subscribe to the `notification_log` table for SMS/Email dispatches.
