CREATE TABLE accounts (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    currency    CHAR(3) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE journal_entries (
    id               BIGSERIAL PRIMARY KEY,
    description      TEXT,
    idempotency_key  TEXT NOT NULL UNIQUE,
    effective_date   DATE NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE postings (
    id                BIGSERIAL PRIMARY KEY,
    journal_entry_id  BIGINT NOT NULL REFERENCES journal_entries(id),
    account_id        BIGINT NOT NULL REFERENCES accounts(id),
    direction         VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_pence      BIGINT NOT NULL CHECK (amount_pence > 0)
);