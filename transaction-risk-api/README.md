# Transaction Risk-Flagging API

A small Spring Boot service that ingests transaction data and flags
suspicious transactions using a rule-based engine — the kind of scaffolding
that sits in front of real fraud/AML monitoring systems in banking IT.

## What it does

- Imports transactions from **any reasonably-shaped CSV** (`POST /api/transactions/import`) — column order doesn't need to match exactly; the importer reads the header row and recognizes common aliases (e.g. `nameOrig` or `account_id` are both understood as the account identifier, `type` or `category` as the merchant category)
- Runs each transaction through a set of independent risk rules
- Persists results to PostgreSQL
- Exposes REST endpoints to query all / flagged / per-account transactions

## Flexible CSV import

Real-world datasets rarely arrive in the exact shape your system wants — a
public dataset like PaySim uses `nameOrig`, `type`, and `step` (hours since
simulation start) instead of `accountId`, `merchantCategory`, and a real
timestamp. Rather than requiring the user to manually reshape every file,
`CsvImportService` reads the header row and matches columns against a table
of known aliases, so differently-shaped files can be dropped in as-is.

A few practical limits worth knowing:
- Import is capped at 3,000 rows per file — the risk engine evaluates one
  transaction at a time (querying that account's history on each row), so an
  unbounded multi-million-row file would take far too long for a demo.
- Rows that can't be parsed at all are skipped and counted, not fatal to the
  whole import.
- If no real date/time column is found, a synthetic timestamp is generated
  (either from a `step`-style hour offset, or just incrementing by one second
  per row) so velocity-style rules still have something to compare against.

## Rules implemented

| Rule | Fires when |
|---|---|
| `HighAmountRule` | A single transaction exceeds €2,500 |
| `VelocityRule` | An account makes 3+ transactions within a 10-minute window |
| `UnusualCategoryRule` | An account (with 5+ prior transactions) transacts in a merchant category it has no history with |

Rules are plain Java implementing a `RiskRule` interface — no DB or Spring
context required to test them — and Spring auto-discovers every `@Component`
that implements it via `List<RiskRule>` injection. Adding a new rule means
adding a new class; no wiring changes.

## Why this design

- **Rules are pure functions** (`Transaction`, history → `Optional<reason>`),
  so they're unit-testable in milliseconds and easy to reason about
  independently — closer to how a real rules engine is structured than one
  giant if/else block.
- **CSV import replays chronologically**, so velocity/category rules see
  accurate history as each row lands, rather than evaluating the whole batch
  against a snapshot.
- **Integration tests use Testcontainers** (a real, disposable Postgres in
  Docker) instead of mocking the repository or using H2 — this catches
  issues that an in-memory DB would hide (real SQL dialect, real
  constraints).

## Running it

```bash
docker-compose up --build
```

This starts Postgres and the API on `localhost:8080`.

Import the sample data:

```bash
curl -F "file=@sample-data/transactions.csv" http://localhost:8080/api/transactions/import
```

Check what got flagged:

```bash
curl http://localhost:8080/api/transactions/flagged
```

## Running tests

```bash
mvn test
```

Unit tests run instantly with no Docker dependency. The integration test
class (`TransactionApiIntegrationTest`) needs Docker running locally, since
it spins up a real Postgres container via Testcontainers.

## What I'd add with more time

- A proper fraud-scoring model instead of hard-coded thresholds (this is
  where the AI/ML side would come in — e.g. an anomaly-detection model
  trained on transaction history per account)
- Pagination on the list endpoints
- Idempotent CSV import (currently re-importing the same file creates
  duplicate rows)
- Basic auth / API key on the import endpoint
