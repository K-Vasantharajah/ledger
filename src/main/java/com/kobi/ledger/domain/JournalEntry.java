package com.kobi.ledger.domain;

import java.util.List;
import java.time.LocalDate;

public record JournalEntry(String idempotencyKey, LocalDate effectiveDate, String description, List<Posting> postings) {

    public JournalEntry {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be blank");
        }

        if (effectiveDate == null) {
            throw new IllegalArgumentException("Effective date must not be null");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }

        if (postings == null || postings.size() < 2) {
            throw new IllegalArgumentException("Journal entry must contain at least two postings");
        }

        long debits = 0;
        long credits = 0;

        for (Posting posting : postings) {
            if (posting.direction() == Direction.DEBIT) {
                debits += posting.amountPence();
            } else {
                credits += posting.amountPence();
            }
        }

        if (debits != credits) {
            throw new IllegalArgumentException(
                    "Unbalanced entry: debits=" + debits + ", credits=" + credits);
        }

        postings = List.copyOf(postings);
    }
}