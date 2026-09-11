# Fixed Deposit Banking Platform

A lab-ready and extensible fixed-deposit platform built as independently deployable services. It covers account opening, interest calculation and accrual, maturity, premature closure, double-entry transaction records, statements, reports, role-based access, notifications, and administrative time travel for demonstrations.

## Architecture

```text
Angular UI :4200
       |
API Gateway :9090 ---- JWT validation and routing
       |                         |
FD Service :8080          Report Service :5000
       |                         |
       +--------- MySQL :3306 ---+
       |
Kafka :9092 ---> Notification Service :5001 ---> Mailpit SMTP :1025
```

Each service has its own Dockerfile. Kubernetes manifests provide separate deployments, services, health probes, resource limits, ingress, and horizontal scaling for the API tier. The report service is separately scalable and can later move to a read replica or reporting database.

## Technology stack

- Java 17, Spring Boot 3.2, Spring Security, JPA, Flyway
- Spring Cloud Gateway and JWT bearer authentication
- Angular 21 with route guards, interceptors, and English and Hindi resources
- Python 3.10, Flask, Pandas, ReportLab, and Matplotlib
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

Demo users all use password `admin123`:

| Username | Role | Main demonstration |
|---|---|---|
| `admin` | ADMIN | Products, batches, time travel, reports |
| `officer1` | BANK_OFFICER | Open an FD and view portfolios |
| `johndoe` | CUSTOMER | View `CUST001`, statements, and withdraw |

Stop the stack with `docker compose down`. Add `-v` only when you intentionally want to erase the local demo database.

## Core capabilities

- Atomic FD creation with branch-based check-digit account numbers and an initial deposit transaction
- Simple and compound-interest simulation, idempotent daily accrual and statements, maturity processing, and manual batch triggers
- Premature-withdrawal penalty calculation with withdrawal and penalty ledger entries
- ADMIN, BANK_OFFICER, and CUSTOMER authorization boundaries
- JSON and CSV reports in Spring plus CSV, PDF, and chart exports in the Python report service
- Versioned Kafka lifecycle events and an independently scalable, idempotent notification consumer, delivered locally to Mailpit
- ISO 4217 handling for INR, USD, EUR, GBP, JPY, AED, and KWD, including 0-, 2-, and 3-decimal currencies
- English and Hindi UI resources
- Health endpoints and container orchestration files

## Service endpoints

| Purpose | Endpoint |
|---|---|
| Register and login | `POST /api/auth/register`, `POST /api/auth/login` |
| Product search | `GET /api/product/search` |
| FD calculation | `POST /api/fd/calculator/simulate` or manual-compatible `POST /api/fd/calculate` |
| Open FD | `POST /api/fd/account/create` or manual-compatible `POST /api/fd/account/create-with-txn` |
| Customer portfolio | `GET /api/fd/accounts/my` |
| Account, transactions, statements | `GET /api/fd/account/{number}/...` |
| Premature closure | `POST /api/fd/account/withdraw` |
| Manual maturity close | `POST /api/fd/account/manual-close` |
| Batch controls | `POST /api/admin/batch/*` |
| Reports | `GET /api/report/*` and `GET /reports/*` through the gateway |

The canonical FD specification is [fd_module_openapi.yaml](fd_module_openapi.yaml). Manual coverage is mapped in [docs/LAB_MANUAL_TRACEABILITY.md](docs/LAB_MANUAL_TRACEABILITY.md), and cross-team dependencies are documented in [docs/INTEGRATION.md](docs/INTEGRATION.md).

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

No paid service is required for the college demonstration. Cloud, real SMTP, SMS, and WhatsApp credentials are deliberately not needed. If the project is deployed later, supply a managed database connection, a random JWT secret, container registry access, and an optional email provider through environment variables.

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
