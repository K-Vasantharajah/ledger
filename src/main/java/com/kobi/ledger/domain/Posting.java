package com.kobi.ledger.domain;

public record Posting(long accountId, Direction direction, long amountPence) {
    public Posting {

        if (amountPence <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amountPence);
        }
    }

    public long effectOn(AccountType type) {
        if (direction == Direction.DEBIT) {
            return type.balance(amountPence, 0);
        } else {
            return type.balance(0, amountPence);
        }
    }
}