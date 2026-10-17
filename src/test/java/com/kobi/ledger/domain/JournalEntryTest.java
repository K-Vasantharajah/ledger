package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JournalEntryTest {

    private static final long SAFEGUARDING_BANK = 1L;
    private static final long CUSTOMER_ALICE = 2L;

    private static JournalEntry entryWith(List<Posting> postings) {
        return new JournalEntry("test-key", LocalDate.of(2026, 10, 17), "Test entry", postings);
    }

    @Test
    void acceptsBalancedEntry() {
        var entry = entryWith(List.of(
                new Posting(SAFEGUARDING_BANK, Direction.DEBIT, 10_000),
                new Posting(CUSTOMER_ALICE, Direction.CREDIT, 10_000)));
        assertEquals(2, entry.postings().size());
    }

    @Test
    void rejectsUnbalancedEntry() {
        assertThrows(IllegalArgumentException.class, () -> entryWith(List.of(
                new Posting(SAFEGUARDING_BANK, Direction.DEBIT, 10_000),
                new Posting(CUSTOMER_ALICE, Direction.CREDIT, 9_000) // £10 missing
        )));
    }

    @Test
    void rejectsEmptyEntry() {
        assertThrows(IllegalArgumentException.class,
                () -> entryWith(List.of()));
    }

    @Test
    void rejectsSinglePosting() {
        assertThrows(IllegalArgumentException.class, () -> entryWith(List.of(
                new Posting(CUSTOMER_ALICE, Direction.DEBIT, 10_000))));
    }
}