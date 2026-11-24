package com.kobi.ledger.domain;

import java.time.LocalDate;

public record StatementLine(String bankTransactionId, LocalDate valueDate, Direction direction,
        long amountPence, String reference, String description) {

    public StatementLine {
        if (bankTransactionId == null || bankTransactionId.isBlank()) {
            throw new IllegalArgumentException("Bank transaction id must not be blank");
        }
        if (valueDate == null) {
            throw new IllegalArgumentException("Value date must not be null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Direction must not be null");
        }
        if (amountPence <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amountPence);
        }
        if (reference != null && reference.isBlank()) {
            reference = null;
        }
    }
}