package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostingTest {

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
}