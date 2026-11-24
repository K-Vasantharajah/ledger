CREATE TABLE statement_lines (
    id                   BIGSERIAL PRIMARY KEY,
    account_id           BIGINT NOT NULL REFERENCES accounts(id),
    bank_transaction_id  TEXT NOT NULL UNIQUE,
    value_date           DATE NOT NULL,
    direction            VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_pence         BIGINT NOT NULL CHECK (amount_pence > 0),
    reference            TEXT,
    description          TEXT,
    imported_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);