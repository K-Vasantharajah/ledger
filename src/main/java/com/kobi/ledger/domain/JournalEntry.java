package com.kobi.ledger.domain;

import java.util.List;

public record JournalEntry(List<Posting> postings) {
    public JournalEntry {
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