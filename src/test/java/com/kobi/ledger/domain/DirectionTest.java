package com.kobi.ledger.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DirectionTest {

    @Test
    void debitOppositeIsCredit() {
        assertEquals(Direction.CREDIT, Direction.DEBIT.opposite());
    }

    @Test
    void creditOppositeIsDebit() {
        assertEquals(Direction.DEBIT, Direction.CREDIT.opposite());
    }

}
