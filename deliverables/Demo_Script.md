# Fixed Deposit Microservice — Demonstration Script

Approximate duration: 75 seconds.

1. **Login:** Open `http://localhost:4200`. Explain that JWT authentication and role-based access are enforced through the API gateway.
2. **Customer dashboard:** Sign in as `johndoe` with password `admin123`. Show the customer-only navigation and summary cards.
3. **Calculator:** Calculate a deposit return. Point out product-based rates, compounding, and currency-aware decimal handling.
4. **Accounts:** Show the customer's FD list, lifecycle status, transactions, and statements.
5. **Officer workflow:** Sign in as `officer1`. Open an FD through the atomic create-with-transaction workflow.
6. **Admin controls:** Sign in as `admin`. Show product management, then trigger daily interest and statement generation. Explain that repeated daily runs are idempotent and that time travel is a forward business-date simulation, not event sourcing.
7. **Reports:** Show the customer portfolio CSV and protected officer/admin summary reports. The independent Python report service calls FD APIs and never reads FD tables directly.
8. **Kafka notification:** Open Mailpit at `http://localhost:8025`. Explain that the FD service publishes a versioned lifecycle event only after the database transaction commits. The independent notification service consumes it, records an idempotent audit row, and sends the email.
9. **Architecture close:** Mention the nine-container Docker demo, Kubernetes manifests and HPAs, and that new Kafka consumers—analytics, audit, or fraud—can be added without changing the FD transaction service.

Submission note: all credentials in the README are local demo accounts only. No paid cloud, SMS, WhatsApp, or live email credentials are required for this demonstration.
