package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JournalEntryTest {

    @Test
    void acceptsBalancedEntry() {
        var entry = new JournalEntry(List.of(
                new Posting("safeguarding-bank", Direction.DEBIT, 10_000),
                new Posting("customer-alice", Direction.CREDIT, 10_000)));
        assertEquals(2, entry.postings().size());
    }

    @Test
    void rejectsUnbalancedEntry() {
        assertThrows(IllegalArgumentException.class, () -> new JournalEntry(List.of(
                new Posting("safeguarding-bank", Direction.DEBIT, 10_000),
                new Posting("customer-alice", Direction.CREDIT, 9_000) // £10 missing
        )));
    }

    @Test
    void rejectsEmptyEntry() {
        assertThrows(IllegalArgumentException.class,
                () -> new JournalEntry(List.of()));
    }

    @Test
    void rejectsSinglePosting() {
        assertThrows(IllegalArgumentException.class, () -> new JournalEntry(List.of(
                new Posting("customer-alice", Direction.DEBIT, 10_000))));
    }
}