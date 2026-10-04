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
4. Log in as `checker1` with `admin123`; approve the request and show the generated account, initial deposit, Kafka events, and captured email.
5. Open the account and show owner snapshot, booked terms, transaction history and lifecycle state.
6. Log in as `admin`; show that Create FD is absent, then trigger interest accrual and statement generation twice to demonstrate the duplicate guard.
7. Open Product Management and explain that products control limits, rates, allowed capitalization/payout frequencies, and premature-closure policy while active FDs retain booked terms.
8. Open the customer statement, notification-service health, and captured emails in Mailpit.
9. Explain that Time Travel is explicitly local test/simulation functionality, then use it or premature withdrawal to show closure date/type, penalty, net payout and the preserved contractual maturity date.
10. Open reports and show active versus closed/matured sections; download the role-appropriate CSV.
11. Log in as `auditor1` and show maker/checker, batch and closure audit records.
12. End with the Kubernetes manifests, health probes, Kafka/outbox flow, and service boundaries that support later scaling.

## Recovery

If demo data becomes inconsistent, run `docker compose down -v` and then start the stack again. This deletes only the Docker demo volume and recreates deterministic seed data.
