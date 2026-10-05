# Fixed Deposit Demonstration Runbook

## Setup

1. Start Docker Desktop.
2. Run `docker compose up --build -d` from the repository root.
3. Confirm every service is healthy with `docker compose ps`.
4. Open the application at http://localhost:4200 and Mailpit at http://localhost:8025.

## Eight minute demonstration

1. Show the architecture diagram and container list.
2. Log in as `johndoe` with `admin123` and run the authenticated FD calculator.
3. Log in as `officer1` with `admin123` and submit an INR FD request for `CUST001`. Point out that no account exists yet.
4. Log in as `checker1` with `admin123`; approve the request and show the generated account, initial deposit, transactional outbox event, Kafka delivery and captured email.
5. Open the account and show owner snapshot, booked terms, transaction history and lifecycle state.
6. Log in as `admin`; show that Create FD is absent. Open Batch Control, point out the Banking Date and trigger interest accrual and statement generation twice. The second attempt is skipped, while Recent Batch Runs shows actor, source, counts and the unique job/date claim.
7. Open Product Management and explain that products control limits, rates, allowed capitalization/payout frequencies, and premature-closure policy while active FDs retain booked terms.
8. Open the customer statement, notification-service health, captured emails in Mailpit, and the separate notification MySQL database.
9. Explain that Time Travel is explicitly local test/simulation functionality. Move forward several days and show that each intervening date is processed using the same accrual/maturity/statement jobs and persistent Banking Clock.
10. Open reports and show active versus closed/matured sections; download the CSV/PDF. Explain that the report came from the Kafka-projected `report_db`, not an FD API or table join.
11. Log in as `auditor1` and show local maker/checker, batch and closure audit records. Then show `/distributed-audit/events` and `audit_db` as the independent event audit.
12. Show the Accounting Service journal for `FD_TRANSACTION_RECORDED`, then use premature withdrawal to explain closure date/type, penalty, net payout, immutable transaction references and preserved contractual maturity date.
13. End with the ER diagram, consumer inboxes, health probes, correlation ID, retry/DLQ and database-per-service boundaries that support independent scaling.

## Recovery

If demo data becomes inconsistent, run `docker compose down -v` and then start the stack again. This deletes only the Docker demo volume and recreates deterministic seed data.
