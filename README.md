# Fixed Deposit Banking Platform

A lab-ready and extensible fixed-deposit platform built as independently deployable services. It covers account opening, interest calculation and accrual, maturity, premature closure, double-entry transaction records, statements, reports, role-based access, notifications, and administrative time travel for demonstrations.

## Architecture

```text
Angular UI :4200
       |
API Gateway :9090 ---- JWT validation, correlation ID and routing
       |
FD Service :8080 ---> FD MySQL (FD aggregates, ledger, batches, outbox)
       |
Outbox Relay ---> Kafka :9092
                    |---> Notification Service :5001 ---> notification_db / Mailpit
                    |---> Reporting Service :5000 ------> report_db
                    |---> Audit Service :5002 ----------> audit_db
                    `---> Accounting Service :5003 -----> accounting_db
```

Each deployable service has its own Dockerfile and data boundary. The four consumers build their own idempotent state from Kafka and have no FD-database credentials. `customer_id`, `product_code` and payout-account references are external identifiers in `fd_accounts`, not cross-service foreign keys. Configurable ports bind Customer and Product/Pricing to either deterministic local-demo adapters or separately deployed team services.

## Technology stack

- Java 21 LTS, Spring Boot 3.5, Spring Security, JPA, Flyway
- Spring Cloud Gateway and JWT bearer authentication
- Angular 21 with route guards, interceptors, and English and Hindi resources
- Python 3.10, Flask, Kafka clients, MySQL Connector, ReportLab, and Matplotlib
- Apache Kafka 3.9, MySQL 8, Docker Compose, and Kubernetes
- Mailpit for free local email demonstrations

## One-command demonstration

Prerequisite: Docker Desktop.

```bash
docker compose up --build -d
docker compose ps
```

Open:

- Application: http://localhost:4200
- API gateway: http://localhost:9090
- FD API Swagger: http://localhost:8080/swagger-ui.html
- Captured email inbox: http://localhost:8025
- Report service health through gateway: http://localhost:9090/reports/health
- Distributed audit health through gateway: http://localhost:9090/distributed-audit/health

Demo users all use password `admin123`:

| Username | Role | Main demonstration |
|---|---|---|
| `admin` | ADMIN | Product management, batches, time travel, reports |
| `officer1` | BANK_OFFICER (Maker) | Submit an FD opening request and view portfolios |
| `checker1` | CHECKER | Independently approve or reject FD opening requests |
| `auditor1` | AUDITOR | Read the append-only FD audit trail |
| `johndoe` | CUSTOMER | Submit a self-owned request; view `CUST001`, statements, and withdraw |

Stop the stack with `docker compose down`. Add `-v` only when you intentionally want to erase the local demo database.

## Core capabilities

- Maker-checker FD opening: customer/officer submission, independent checker decision, then idempotent atomic account creation, deposit, and transactional outbox event
- Simple and compound-interest simulation, idempotent daily accrual and statements, maturity processing, and manual batch triggers
- Premature-withdrawal penalty calculation with withdrawal and penalty ledger entries
- CUSTOMER, BANK_OFFICER/Maker, CHECKER, ADMIN, and AUDITOR authorization boundaries with segregation of duties
- Owner snapshots, explicit ACTIVE/CLOSED/PREMATURE_CLOSED/RENEWED lifecycle, closure facts, and append-only audit records
- Protected Angular product-management screen for product limits, currencies, rates, frequencies, penalties, and premature-closure policy
- Event-projected JSON, CSV, PDF, and chart reports from a dedicated reporting database
- Independent Kafka-driven audit and accounting services with consumer inbox deduplication
- Versioned Kafka lifecycle events, transactional outbox relay, consumer inbox, retry/DLQ handling, and local Mailpit delivery
- Persistent Banking Clock, automatic Beginning-of-Day catch-up, auditable batch-run metadata, single distributed claim per job/business date, and account-level duplicate protection
- ISO 4217 handling for INR, USD, EUR, GBP, JPY, AED, and KWD, including 0-, 2-, and 3-decimal currencies
- English and Hindi UI resources
- Health endpoints and container orchestration files

## Service endpoints

| Purpose | Endpoint |
|---|---|
| Register and login | `POST /api/auth/register`, `POST /api/auth/login` |
| Product search | `GET /api/product/search` |
| FD calculation | `POST /api/fd/calculator/simulate` or manual-compatible `POST /api/fd/calculate` |
| Submit FD opening | `POST /api/fd/opening-requests` with required `Idempotency-Key` (CUSTOMER/BANK_OFFICER) |
| Review FD opening | `GET /api/fd/opening-requests/pending`, `POST /api/fd/opening-requests/{id}/approve|reject` (CHECKER) |
| Customer portfolio | `GET /api/fd/accounts/my` |
| Account, transactions, statements | `GET /api/fd/account/{number}/...` |
| Premature closure | `POST /api/fd/account/withdraw` |
| Manual maturity close | `POST /api/fd/account/manual-close` |
| Batch controls | `POST /api/admin/batch/*` |
| Banking date and run history | `GET /api/admin/business-date`, `GET /api/admin/batch/runs` |
| Audit trail | `GET /api/audit/recent` (ADMIN/AUDITOR) |
| Distributed event audit | `GET /distributed-audit/events` through gateway |
| Accounting journal | `GET /accounting/entries` through gateway (ADMIN/AUDITOR) |
| Reports | `GET /api/report/*` and `GET /reports/*` through the gateway |
| Customer portfolio CSV | `GET /api/report/customer-portfolio/export/csv` |

The canonical HTTP specification is [fd_module_openapi.yaml](fd_module_openapi.yaml), the event specification is [docs/fd-lifecycle-asyncapi.yaml](docs/fd-lifecycle-asyncapi.yaml), and the service-owned database model is [fd-microservice/DATABASE_ER_DIAGRAM.md](fd-microservice/DATABASE_ER_DIAGRAM.md).

## Local development

Backend tests:

```bash
cd fd-microservice
mvn test
```

Frontend:

```bash
cd fd-angular-ui
npm ci
npm start
```

The Angular development server proxies `/api` to the gateway at port 9090. Production dependencies currently audit with zero known vulnerabilities; remaining audit notices belong to the development server toolchain.

## Configuration and credentials

Copy `.env.example` to `.env` only when changing local defaults. Never commit `.env` or `k8s/secret.yaml`. Kubernetes uses [k8s/secret.example.yaml](k8s/secret.example.yaml) as a template.

No paid service is required for the college demonstration. Cloud, real SMTP, SMS, and WhatsApp credentials are deliberately not needed. The time-travel simulator is disabled by default and explicitly enabled only in the local Docker demonstration; it advances the Banking Clock one day at a time through the real batch functions. If the project is deployed later, supply managed service databases, a secured Kafka cluster, a random JWT secret, container registry access, team Identity/Customer/Product endpoints, and an optional email provider through environment variables.

## Project deliverables

Submission artifacts live under `deliverables/`:

- `Fixed_Deposit_Microservice_Report.docx` and `.pdf`
- `Fixed_Deposit_Microservice_Presentation_Final.pptx`
- `Fixed_Deposit_Microservice_Demonstration.mp4` and `Demo_Script.md`
- Test evidence, integration contract, OpenAPI contract, and lab-manual traceability under `docs/`

## Repository hygiene

Generated dependencies and build output are excluded from Git. Use `npm ci` and Maven to reproduce them. CI verifies Java tests, the Angular production build and audit, Python syntax, and Docker Compose configuration.

## License

MIT
