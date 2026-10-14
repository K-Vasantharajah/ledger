package com.kobi.ledger.domain;

import java.util.List;

public record JournalEntry(List<Posting> postings) {
    public JournalEntry {
        if (postings.size() < 2) {
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