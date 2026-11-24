package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StatementLineTest {

    @Test
    void acceptsValidLine() {
        var line = new StatementLine("BANK-001", LocalDate.of(2026, 11, 13), Direction.CREDIT,
                10_000, "ALICE-TOPUP", "ALICE SMITH");
        assertEquals("ALICE-TOPUP", line.reference());
    }

    @Test
    void rejectsBlankBankTransactionId() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new StatementLine(" ", LocalDate.of(2026, 11, 13), Direction.CREDIT,
                        10_000, "ALICE-TOPUP", "ALICE SMITH"));
        assertEquals("Bank transaction id must not be blank", ex.getMessage());
    }

    @Test
    void rejectsNullValueDate() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new StatementLine("BANK-001", null, Direction.CREDIT,
                        10_000, "ALICE-TOPUP", "ALICE SMITH"));
        assertEquals("Value date must not be null", ex.getMessage());
    }

    @Test
    void rejectsNullDirection() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new StatementLine("BANK-001", LocalDate.of(2026, 11, 13), null,
                        10_000, "ALICE-TOPUP", "ALICE SMITH"));
        assertEquals("Direction must not be null", ex.getMessage());
    }

    @Test
    void rejectsZeroAmount() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new StatementLine("BANK-001", LocalDate.of(2026, 11, 13), Direction.CREDIT,
                        0, "ALICE-TOPUP", "ALICE SMITH"));
        assertEquals("Amount must be positive: 0", ex.getMessage());
    }

    @Test
    void allowsMissingReferenceAndDescription() {
        var line = new StatementLine("BANK-001", LocalDate.of(2026, 11, 13), Direction.CREDIT,
                10_000, null, null);
        assertNull(line.reference());
        assertNull(line.description());
    }

    @Test
    void treatsBlankReferenceAsMissing() {
        var line = new StatementLine("BANK-001", LocalDate.of(2026, 11, 13), Direction.CREDIT,
                10_000, " ", "ALICE SMITH");
        assertNull(line.reference());
        assertEquals("ALICE SMITH", line.description());
    }
}