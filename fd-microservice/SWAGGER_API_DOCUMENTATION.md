# 📖 Fixed Deposit Microservice — API Specification & Integration Guide

This document contains the complete **REST API Specification** for the **Fixed Deposit (FD) Microservice**. It is formatted for seamless sharing with frontend and cross-service backend team members (Customer Onboarding, Core Banking, Reporting).

---

## 📌 General Information

- **Base URL:** `http://localhost:8080/api`
- **Swagger Interactive UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3.0 JSON Spec File:** [`openapi_spec.json`](file:///c:/Users/krish/OneDrive/Desktop/BT%20lab/fd-microservice/openapi_spec.json)
- **Data Interchange Format:** `application/json`
- **Authentication Scheme:** `Bearer <JWT_TOKEN>` via `Authorization` header

---

## 🔑 Authentication & Authorization

All protected endpoints require a valid JWT token passed in the Request Header:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Roles Supported:
1. **`CUSTOMER`**: Can calculate interest, open FD accounts, view own portfolio, view own statements/transactions, and request premature withdrawal on own accounts.
2. **`BANK_OFFICER`**: Can create FD accounts for customers, trigger manual closures, view all customer accounts, and export reports.
3. **`ADMIN`**: Full administrative access — create/update products, execute batch jobs on demand, perform time-travel simulations, and export compliance CSV reports.

---

## 🌐 API Endpoint Catalog

---

### 1. Authentication Endpoints (`/api/auth`)

#### 1.1 User Registration
- **Endpoint:** `POST /api/auth/register`
- **Access:** Public
- **Description:** Registers a new user with encrypted password storage (Bcrypt).
- **Request Body:**
```json
{
  "username": "johndoe",
  "password": "Password@123",
  "email": "johndoe@example.com",
  "role": "CUSTOMER"
}
```
- **Response (200 OK):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "username": "johndoe",
    "role": "CUSTOMER"
  },
  "timestamp": "2026-08-30T12:00:00"
}
```

#### 1.2 User Login
- **Endpoint:** `POST /api/auth/login`
- **Access:** Public
- **Description:** Authenticates user credentials and returns a signed JWT token containing `role` and `customerId` claims.
- **Request Body:**
```json
{
  "username": "johndoe",
  "password": "Password@123"
}
```
- **Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "johndoe",
  "role": "CUSTOMER",
  "customerId": "CUST001"
}
```

---

### 2. Product & Pricing Endpoints (`/api/product`)

#### 2.1 Get All Products
- **Endpoint:** `GET /api/product/all`
- **Access:** Public
- **Description:** Returns all banking product definitions (used by UI dropdowns).
- **Response (200 OK):**
```json
[
  {
    "productCode": "FD_STD",
    "productName": "Standard Fixed Deposit",
    "productType": "FD",
    "currency": "INR",
    "effectiveDate": "2025-01-01",
    "minTermMonths": 3,
    "maxTermMonths": 36,
    "minRate": 6.50,
    "maxRate": 8.50,
    "minDeposit": 1000.00,
    "rateCapAddon": 2.00,
    "preMaturityPenaltyPct": 1.00,
    "compoundingFrequency": "QUARTERLY",
    "status": "ACTIVE"
  }
]
```

#### 2.2 Search Products by Filter
- **Endpoint:** `GET /api/product/search?type=FD&status=ACTIVE`
- **Access:** Public
- **Query Parameters:** `type` (default `FD`), `status` (default `ACTIVE`)
- **Response (200 OK):** Array of matching `Product` objects.

#### 2.3 Get Single Product Detail
- **Endpoint:** `GET /api/product/{productCode}`
- **Access:** Public
- **Response (200 OK):** Single `Product` object.

#### 2.4 Create Product
- **Endpoint:** `POST /api/product`
- **Access:** Restricted to `ADMIN`
- **Request Body:**
```json
{
  "productCode": "FD_PREM",
  "productName": "Premium High-Yield FD",
  "productType": "FD",
  "currency": "INR",
  "effectiveDate": "2025-01-01",
  "minTermMonths": 12,
  "maxTermMonths": 60,
  "minRate": 7.25,
  "maxRate": 9.00,
  "minDeposit": 50000.00,
  "rateCapAddon": 2.00,
  "preMaturityPenaltyPct": 1.00,
  "compoundingFrequency": "QUARTERLY"
}
```
- **Response (200 OK):** `ApiResponse` with created product data.

#### 2.5 Update Product
- **Endpoint:** `PUT /api/product/{productCode}`
- **Access:** Restricted to `ADMIN`
- **Request Body:** Partial or complete `ProductRequest` object.

---

### 3. FD Calculator Endpoint (`/api/fd`)

#### 3.1 Calculate Maturity & Interest
- **Endpoint:** `POST /api/fd/calculate`
- **Access:** Restricted to `CUSTOMER`
- **Description:** Simulates maturity amount and interest earned using Simple or Compound formulas with category add-on rate bonuses.
- **Request Body:**
```json
{
  "principal": 100000.00,
  "termMonths": 12,
  "baseRate": 6.50,
  "compoundingFrequency": "QUARTERLY",
  "calculationType": "COMPOUND",
  "categories": ["SENIOR_CITIZEN", "STAFF"]
}
```
- **Response (200 OK):**
```json
{
  "principal": 100000.00,
  "effectiveRate": 8.00,
  "tenureMonths": 12,
  "compoundingFrequency": "QUARTERLY",
  "calculationType": "COMPOUND",
  "maturityAmount": 108243.22,
  "interestEarned": 8243.22,
  "categoryAddons": {
    "SENIOR_CITIZEN": 0.50,
    "STAFF": 1.00
  },
  "totalAddon": 1.50
}
```

---

### 4. FD Account Management Endpoints (`/api/fd/account`)

#### 4.1 Create FD Account (Book FD)
- **Endpoint:** `POST /api/fd/account/create`
- **Access:** Restricted to `BANK_OFFICER` or `ADMIN`
- **Description:** Creates an FD account, generates a 10-digit account number `[branch][seq][checksum]`, and logs an initial DEPOSIT transaction in a single database transaction.
- **Request Body:**
```json
{
  "customerId": "CUST001",
  "productCode": "FD_STD",
  "principalAmount": 100000.00,
  "termMonths": 12,
  "branchCode": "001",
  "currency": "INR",
  "categories": ["SENIOR_CITIZEN"]
}
```
- **Response (200 OK):**
```json
{
  "fdAccountNo": "00100000254",
  "customerId": "CUST001",
  "productCode": "FD_STD",
  "currency": "INR",
  "principalAmount": 100000.00,
  "interestRate": 7.00,
  "tenureMonths": 12,
  "compoundingFrequency": "QUARTERLY",
  "status": "ACTIVE",
  "maturityDate": "2026-08-30",
  "accruedInterest": 0.00,
  "initialTransactionId": 1045,
  "message": "Account created successfully with initial deposit"
}
```

#### 4.2 Get My FD Accounts
- **Endpoint:** `GET /api/fd/accounts/my`
- **Access:** Restricted to `CUSTOMER`
- **Description:** Retrieves all FD accounts owned by the authenticated customer (extracted automatically from JWT token).
- **Response (200 OK):** Array of `FdAccountResponse` objects.

#### 4.3 Get All FD Accounts
- **Endpoint:** `GET /api/fd/accounts/all`
- **Access:** Restricted to `BANK_OFFICER` or `ADMIN`
- **Response (200 OK):** Array of all `FdAccountResponse` objects in system.

#### 4.4 Get Single FD Account
- **Endpoint:** `GET /api/fd/account/{fdAccountNo}`
- **Access:** Authenticated
- **Response (200 OK):** Single `FdAccountResponse` object.

#### 4.5 Get Account Transaction History
- **Endpoint:** `GET /api/fd/account/{fdAccountNo}/transactions`
- **Access:** Authenticated
- **Description:** Retrieves the append-only transaction ledger history for the account.
- **Response (200 OK):** Array of `FdTransaction` objects with Debit/Credit GL accounts.

#### 4.6 Get Account Monthly Statements
- **Endpoint:** `GET /api/fd/account/{fdAccountNo}/statements`
- **Access:** Authenticated
- **Response (200 OK):** Array of `FdStatement` summary objects.

#### 4.7 Request Premature Withdrawal
- **Endpoint:** `POST /api/fd/account/withdraw`
- **Access:** Restricted to `CUSTOMER` or `BANK_OFFICER`
- **Description:** Performs early closure of an active FD account, calculates accrued interest till today, deducts product penalty %, updates account status to `PREMATURE_CLOSED`, and logs `WITHDRAWAL` and `PENALTY` transactions.
- **Request Body:**
```json
{
  "fdAccountNo": "00100000254",
  "withdrawalDate": "2026-08-30",
  "transferAccount": "SB000789",
  "remarks": "Emergency withdrawal request"
}
```
- **Response (200 OK):**
```json
{
  "status": "PREMATURE_CLOSED",
  "message": "FD Account prematurely closed with penalty applied",
  "fdAccountNo": "00100000254",
  "withdrawalAmount": 103500.00,
  "principalReturned": 100000.00,
  "interestEarned": 4000.00,
  "penaltyApplied": 500.00,
  "effectiveRate": 7.00
}
```

#### 4.8 Manual Maturity Closure
- **Endpoint:** `POST /api/fd/account/manual-close?fdAccountNo=00100000254`
- **Access:** Restricted to `BANK_OFFICER` or `ADMIN`
- **Description:** Closes a matured FD account and processes payout. (Rejects early closure requests and directs caller to withdrawal endpoint).

---

### 5. Operational Reports Endpoints (`/api/report`)

#### 5.1 FD Summary Report
- **Endpoint:** `GET /api/report/fd-summary`
- **Access:** Restricted to `BANK_OFFICER` or `ADMIN`
- **Response (200 OK):**
```json
[
  {
    "productCode": "FD_STD",
    "productName": "Standard Fixed Deposit",
    "totalAccounts": 15,
    "activeAccounts": 12,
    "closedAccounts": 3,
    "totalPrincipal": 1500000.00,
    "totalInterestAccrued": 45200.00
  }
]
```

#### 5.2 Customer Portfolio Report
- **Endpoint:** `GET /api/report/customer-portfolio`
- **Access:** Restricted to `CUSTOMER`
- **Response (200 OK):** Array of `FdPortfolioReport` items.

#### 5.3 Export Summary CSV
- **Endpoint:** `GET /api/report/export/csv`
- **Access:** Restricted to `BANK_OFFICER` or `ADMIN`
- **Response (200 OK):** File download `fd_summary_report.csv` (`text/csv`).

---

### 6. Admin Batch Job Control Endpoints (`/api/admin`)

#### 6.1 Trigger Daily Interest Accrual Batch
- **Endpoint:** `POST /api/admin/batch/interest-accrual`
- **Access:** Restricted to `ADMIN`

#### 6.2 Trigger Daily Maturity Processing Batch
- **Endpoint:** `POST /api/admin/batch/maturity-processing`
- **Access:** Restricted to `ADMIN`

#### 6.3 Trigger Monthly Statement Generation Batch
- **Endpoint:** `POST /api/admin/batch/statement-generation`
- **Access:** Restricted to `ADMIN`

#### 6.4 Fast-Forward System Date (Time Travel Simulation)
- **Endpoint:** `POST /api/admin/time-travel`
- **Access:** Restricted to `ADMIN`
- **Request Body:**
```json
{
  "targetDate": "2026-12-31",
  "operation": "ADVANCE_AND_RUN_BATCHES"
}
```

---

## ⚠️ Common Error Response Structure

All exceptions return a standardized JSON error response:

```json
{
  "success": false,
  "message": "Product is not ACTIVE: FD_EXPIRED",
  "data": null,
  "timestamp": "2026-08-30T12:26:00"
}
```

### Standard HTTP Status Codes:
- `200 OK`: Request succeeded.
- `400 Bad Request`: Validation failure or invalid parameter.
- `401 Unauthorized`: Missing or invalid JWT token.
- `403 Forbidden`: Authenticated user lacks required role (`CUSTOMER`, `OFFICER`, `ADMIN`).
- `404 Not Found`: Account or Product code does not exist.
- `409 Conflict`: Duplicate entry (e.g., username already registered).
