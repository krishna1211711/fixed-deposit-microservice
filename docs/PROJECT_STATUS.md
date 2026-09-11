# Project Completion Status

## Completed in this repository

- Fixed-deposit calculation, product validation, account opening with atomic initial transaction, lifecycle queries, statements, premature withdrawal, maturity closure, and protected reports.
- JWT login plus CUSTOMER, BANK_OFFICER, and ADMIN authorization through the API gateway.
- ISO-style three-letter currencies with currency-specific decimal precision and seeded INR, USD, EUR, GBP, JPY, AED, and KWD products.
- Daily interest and daily statement jobs with duplicate-run protection; administrative manual triggers remain available for demonstrations.
- Kafka topic `fd.lifecycle.v1`, after-commit event publication, stable account keys, UUID event IDs, and a versioned JSON Schema contract.
- Independent Python notification microservice with a Kafka consumer group, event-ID idempotency, notification audit records, SMTP delivery, and health reporting.
- Independent protected report microservice.
- Eight-service Docker Compose demonstration with MySQL, Kafka, Mailpit, health checks, and deterministic demo users/data.
- Kubernetes manifests for every module, probes, resource bounds, and HPAs for FD, report, and notification services.
- Maven, Angular, Python, Compose, API authorization, scheduler-idempotency, and end-to-end Kafka verification.
- OpenAPI, integration contract, lab-manual traceability, test evidence, demo runbook, technical report, PDF, presentation, narrated video, and recording script.
- CI workflow and repository cleanup so generated dependencies, build output, secrets, and temporary artifact tooling are not committed.

## Intentionally left for integration/deployment time

- Contract testing against other groups' runnable Auth, Customer, Product, and Calculation services. Their supplied repository defines contracts but does not yet provide every runnable dependency.
- Selecting and provisioning a cloud subscription. Local Docker is the required zero-cost path; Azure for Students can be evaluated later without changing the service boundaries.
- Replacing Mailpit with a real email provider. This is optional and not required for the lab demonstration.
- Production hardening beyond the project scope: managed Kafka, TLS, secret manager, database backups/read replicas, observability, transactional outbox, dead-letter topic, and load testing.
- Professor submission or team merge approval, which must be coordinated by the group leader.

## Credentials and actions needed from the group leader

- **Now:** authenticate GitHub in the browser only if Git Credential Manager prompts during push.
- **Before group integration:** obtain the other groups' runnable branch/tag, base URLs, test users, and any public-key/JWT issuer details they choose.
- **Before cloud deployment:** provide access to the selected student cloud subscription and container registry. Do not send passwords in chat or commit them; use the provider login and secret store.
- **For real email only:** provide a verified sender/domain and API key through deployment secrets. Mailpit needs no credentials.
- **For submission:** confirm student names, registration numbers, course/section, professor name, and any institution cover-page format so the final report/deck metadata can be personalized.
