package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostingTest {

    private static final long SAFEGUARDING_BANK = 1L;
    private static final long CUSTOMER_ALICE = 2L;

    @Test
    void rejectsZeroAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Posting(CUSTOMER_ALICE, Direction.DEBIT, 0));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Posting(CUSTOMER_ALICE, Direction.DEBIT, -500));
    }

    @Test
    void debitReducesLiabilityBalance() {
        var posting = new Posting(CUSTOMER_ALICE, Direction.DEBIT, 1_000);
        assertEquals(-1_000, posting.effectOn(AccountType.LIABILITY));
    }

    @Test
    void creditIncreasesLiabilityBalance() {
        var posting = new Posting(CUSTOMER_ALICE, Direction.CREDIT, 1_000);
        assertEquals(1_000, posting.effectOn(AccountType.LIABILITY));
    }

    @Test
    void debitIncreasesAssetBalance() {
        var posting = new Posting(SAFEGUARDING_BANK, Direction.DEBIT, 1_000);
        assertEquals(1_000, posting.effectOn(AccountType.ASSET));
    }
}