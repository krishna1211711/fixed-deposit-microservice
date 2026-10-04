# Fixed Deposit Demonstration Runbook

## Setup

1. Start Docker Desktop.
2. Run `docker compose up --build -d` from the repository root.
3. Confirm every service is healthy with `docker compose ps`.
4. Open the application at http://localhost:4200 and Mailpit at http://localhost:8025.

## Eight minute demonstration

1. Show the architecture diagram and container list.
2. Log in as `johndoe` with `admin123` and run the authenticated FD calculator.
3. Log in as `officer1` with `admin123`.
4. Create an INR FD for `CUST001`; point out the generated account number, initial deposit transaction, Kafka event, and captured opening email.
5. Open the account and show its transaction history.
6. Log in as `admin`; trigger interest accrual and statement generation.
7. Open Product Management and explain that products control limits, rates, allowed capitalization/payout frequencies, and premature-closure policy while active FDs retain booked terms.
8. Open the customer statement, notification-service health, and captured emails in Mailpit.
9. Use time travel or the premature-withdrawal flow to demonstrate a lifecycle transition and penalty entry. Single-operation time travel executes only the selected operation.
10. Open reports and download the role-appropriate customer portfolio or staff summary CSV.
11. End with the Kubernetes manifests, health probes, and service boundaries that support later scaling.

## Recovery

If demo data becomes inconsistent, run `docker compose down -v` and then start the stack again. This deletes only the Docker demo volume and recreates deterministic seed data.
