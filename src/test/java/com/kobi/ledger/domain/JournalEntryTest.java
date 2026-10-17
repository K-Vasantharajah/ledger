package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JournalEntryTest {

    private static final long SAFEGUARDING_BANK = 1L;
    private static final long CUSTOMER_ALICE = 2L;

    @Test
    void acceptsBalancedEntry() {
        var entry = new JournalEntry(List.of(
                new Posting(SAFEGUARDING_BANK, Direction.DEBIT, 10_000),
                new Posting(CUSTOMER_ALICE, Direction.CREDIT, 10_000)));
        assertEquals(2, entry.postings().size());
    }

    @Test
    void rejectsUnbalancedEntry() {
        assertThrows(IllegalArgumentException.class, () -> new JournalEntry(List.of(
                new Posting(SAFEGUARDING_BANK, Direction.DEBIT, 10_000),
                new Posting(CUSTOMER_ALICE, Direction.CREDIT, 9_000) // £10 missing
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
                new Posting(CUSTOMER_ALICE, Direction.DEBIT, 10_000))));
    }
}