package com.kobi.ledger.domain;

public record Posting(String accountId, Direction direction, long amountPence) {
    public Posting {

        if (amountPence <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amountPence);
        }
    }
}