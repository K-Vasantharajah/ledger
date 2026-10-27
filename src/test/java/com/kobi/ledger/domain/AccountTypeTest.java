package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTypeTest {

    @Test
    void equityBalanceIsCreditsMinusDebits() {
        long debits = 200;
        long credits = 500;

        assertEquals(300, AccountType.EQUITY.balance(debits, credits));
    }

    @Test
    void aliceBalanceAfterTopUpAndPayment() {
        long debits = 3_050;
        long credits = 10_000;

        assertEquals(6_950, AccountType.LIABILITY.balance(debits, credits));
    }

    @Test
    void safeguardingBankBalanceAfterTopUpAndPayment() {
        long debits = 10_000;
        long credits = 0;

        assertEquals(10_000, AccountType.ASSET.balance(debits, credits));
    }

    @Test
    void feeRevenueBalanceAfterTopUpAndPayment() {
        long debits = 0;
        long credits = 50;

        assertEquals(50, AccountType.REVENUE.balance(debits, credits));
    }
}