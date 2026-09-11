# Cross Team Integration Contract

## Sources reviewed

The integration baseline is the public `BT-Team-1` repository supplied by the team lead. It contains `Swagger_API.yaml`, `ER_Diagram.pdf`, and `Work_Distribution_TEAM_1.xlsx`. The contract lists Authentication on port 3020, Product and Pricing on port 8080, FD Calculation on port 4030, Customer on port 1005, and FD Account Management on port 9090.

## Implemented mappings

| Shared capability | This repository | Status |
|---|---|---|
| JWT login and registration | `/api/auth/login`, `/api/auth/register` | Implemented locally; replaceable by team Auth service |
| Product search and retrieval | `/api/product/search`, `/api/product/{code}` | Implemented; product code remains the stable key |
| FD calculation | `/api/fd/calculator/simulate`, `/api/fd/calculate` | Implemented locally; second path preserves the manual contract |
| FD account creation | `/api/fd/account/create`, `/api/fd/account/create-with-txn` | Implemented with product validation and atomic initial deposit |
| Transactions | `/api/fd/account/{number}/transactions` | Implemented with GL debit and credit accounts |
| Statements | `/api/fd/account/{number}/statements` | Implemented |
| Premature withdrawal | `/api/fd/account/withdraw` | Implemented with penalty audit entry |
| Interest and maturity jobs | `/api/admin/batch/*` | Implemented with manual triggers |
| Reports | `/api/report/*` and `/reports/*` | Implemented in Java and Python services |

## Contract differences

The shared Swagger creates an FD with `{accountName, calcId}` at `POST /api/v1/accounts`. It expects the FD service to fetch the customer profile, calculation, and product from three other running services. Those repositories and response payloads are not present in the supplied team repository, so a live adapter cannot be verified yet.

This module currently accepts the resolved values at `POST /api/fd/account/create`: `customerId`, `productCode`, `principalAmount`, `termMonths`, `branchCode`, `currency`, and optional customer categories. This is the stable standalone demonstration path.

Before the final team merge, agree on these points:

1. Exact calculation lookup path and response fields for `calcId`.
2. Customer lookup path, JWT subject meaning, and canonical `customerId` field.
3. Product payload fields for rate, currency, limits, penalty, and compounding.
4. Whether the shared `/api/v1` paths stay canonical or the gateway rewrites them to this module's `/api/fd` paths.
5. Event contract ownership. This repository publishes versioned lifecycle messages to Kafka topic `fd.lifecycle.v1`; other groups may add independent consumer groups without changing FD transaction logic.
6. Savings or payment API for maturity and withdrawal payouts. Current ledger entries simulate the transfer and preserve an audit trail.

## Recommended team integration

Use the team API gateway as the single browser entry point. Keep authentication in the Auth service, product rules in Product and Pricing, calculations in the calculator, and lifecycle state in this FD service. Pass JWTs through unchanged. Configure external service base URLs by environment variable and use timeouts, retries only for safe reads, and idempotency keys for account opening and payouts. Kafka is the asynchronous boundary for notifications and later analytics, audit, or fraud consumers.

The ER diagram aligns on `users`, customer details, `products`, `fd_accounts`, `fd_transactions`, `fd_interest_transactions`, `fd_statements`, and `notification_log`. This module uses those stable identifiers and preserves `product_code`, `customer_id`, and `fd_account_no` for cross-service references.

The machine-readable Kafka contract is in `docs/events/fd-lifecycle-v1.schema.json`. Messages are keyed by `fdAccountNo`, use schema version `1.0`, and carry a UUID `eventId` so every consumer can implement idempotency independently.

## External validation still required

Run a contract test after the Auth, Customer, Product, and Calculation groups publish runnable branches. Validate one successful account opening, an invalid calculation, an unavailable dependency, duplicate submission, token expiry, and a customer attempting to access another customer's account.
