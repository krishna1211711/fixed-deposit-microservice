# Fixed Deposit Microservice — Demonstration Script

Approximate duration: 75 seconds.

1. **Login:** Open `http://localhost:4200`. Explain that JWT authentication and role-based access are enforced through the API gateway.
2. **Customer dashboard:** Sign in as `johndoe` with password `admin123`. Show the customer-only navigation and summary cards.
3. **Calculator:** Calculate a deposit return. Point out product-based rates, compounding, and currency-aware decimal handling.
4. **Accounts:** Show the customer's FD list, lifecycle status, transactions, and statements.
5. **Maker-checker workflow:** Sign in as `officer1` and submit an opening request. Sign in as `checker1`; approve it and show that account/deposit creation occurs only after the independent decision.
6. **Admin controls:** Sign in as `admin`. Show that Create FD is absent, configure product stackability, then trigger daily interest and statement generation. Explain that repeated daily runs are idempotent and audited; time travel is local test simulation only.
7. **Reports:** Show active versus closed/matured portfolios, CSV/PDF export and charts. The independent Python report service consumes Kafka events into `report_db`; it neither calls FD APIs nor reads FD tables.
8. **Kafka notification:** Open Mailpit at `http://localhost:8025`. Explain that the FD service publishes a versioned lifecycle event only after the database transaction commits. The independent notification service consumes it, records an idempotent audit row, and sends the email.
9. **Audit and architecture close:** Sign in as `auditor1`, show the audit trail, and mention the nine-container Docker demo, Kubernetes manifests/HPAs, and that new Kafka consumers—analytics, enterprise audit, or fraud—can be added without changing the FD transaction service.

Submission note: all credentials in the README are local demo accounts only. No paid cloud, SMS, WhatsApp, or live email credentials are required for this demonstration.
