package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostingTest {

    

    @Test
    void rejectsZeroAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Posting("customer-alice", Direction.DEBIT, 0));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Posting("customer-alice", Direction.DEBIT, -500));
    }
}