# Ledger

A double-entry ledger service in Java, modelled on the books of a small e-money
wallet provider. Every movement of money is recorded as a balanced journal entry,
history is append-only, and the API is safe to retry.

## Features

- **Double-entry bookkeeping:** every entry's debits equal its credits, enforced in
  the domain model and backed by database constraints
- **Atomic posting:** an entry and all its postings are saved in one transaction,
  or not at all
- **Idempotent API:** retrying a request with the same `Idempotency-Key` returns the
  original result, including when two identical requests race
- **Reversals:** mistakes are corrected by posting a mirror-image entry; each entry
  can be reversed at most once
- **Balances** calculated from postings, respecting each account type's normal balance
- **Errors** returned as RFC 9457 problem details with meaningful status codes

## Tech stack

Java 21, Spring Boot 4, PostgreSQL 16, Flyway, Testcontainers, GitHub Actions.

## API

| Method | Path                             | Description                                                                                      |
| ------ | -------------------------------- | ------------------------------------------------------------------------------------------------ |
| `POST` | `/accounts`                      | Create an account → `201`                                                                        |
| `GET`  | `/accounts/{id}/balance`         | Account balance in pence → `200`, or `404`                                                       |
| `POST` | `/journal-entries`               | Post a balanced entry → `201`; `400` if invalid, `409` if the key was used for a different entry |
| `POST` | `/journal-entries/{id}/reversal` | Reverse an entry → `201`; `404` if unknown, `409` if already reversed                            |

Example: Alice pays Bob £30.

```http
POST /journal-entries
Idempotency-Key: pay-7f3a9
Content-Type: application/json

{
  "effectiveDate": "2026-11-12",
  "description": "Alice pays Bob £30",
  "postings": [
    { "accountId": 2, "direction": "DEBIT",  "amountPence": 3000 },
    { "accountId": 3, "direction": "CREDIT", "amountPence": 3000 }
  ]
}
```

## Design decisions

- **Debit/credit with positive amounts, not signed numbers.** This matches how
  accounting and ledger APIs model postings, and keeps "which side" separate from
  "how much". Whether a debit increases a balance depends on the account type.
- **Money as `BIGINT` minor units (pence).** There's no floating point anywhere, so no
  rounding errors. JSON carries integer pence too, because decimal JSON numbers
  usually become floats on the other side.
- **Append-only, with corrections as reversals.** Entries are never edited or
  deleted, so the history is the audit trail. A `UNIQUE` constraint on
  `reversal_of_id` stops an entry being reversed twice.
- **The idempotency key is in an HTTP header.** It describes the request, not the
  money movement, and it follows the convention used by payment APIs such as Stripe.
  The key comes from the caller, because only the caller knows whether two requests
  are the same one.
- **Plain SQL with `JdbcClient`, not JPA.** Domain objects are immutable records,
  and in a ledger I want to see exactly which SQL runs inside which transaction.
- **Rules live in the domain, backed up by the database.** Pure Java classes enforce
  the invariants, and Postgres constraints (`CHECK`, `UNIQUE`, foreign keys) back
  them up, so a bug in application code still can't corrupt the books.

## Running the tests

Prerequisites: Java 21 and Docker (it must be running).

```bash
./mvnw test
```

Integration tests run against a real PostgreSQL 16 container, with the schema built
from the Flyway migrations on every run.

## Roadmap

- [x] Milestone 1: single-currency ledger core, idempotent API, reversals, CI
- [ ] Milestone 2: concurrency (a no-overdraft rule under simultaneous spends)
- [ ] Milestone 3: reconciliation against external bank statements
