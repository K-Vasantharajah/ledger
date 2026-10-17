# Ledger

A double-entry ledger and reconciliation service in Java.

Goals: model accounts and balanced transactions correctly, reconcile internal
records against external statements, and handle the edge cases payments
systems care about: idempotency, money precision, and auditability.

## Tech stack

Java 21, Spring Boot 4, PostgreSQL 16, Flyway, Testcontainers.

## Running the tests

Prerequisites: Java 21 and Docker (it must be running).

```bash
./mvnw test
```

Integration tests run against a real PostgreSQL 16 container, with the schema
built from the Flyway migrations on every run.

Work in progress.
