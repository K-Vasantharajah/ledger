package com.kobi.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void acceptsValidAccount() {
        var account = new Account("customer:alice:gbp", AccountType.LIABILITY, "GBP");
        assertEquals("customer:alice:gbp", account.name());
        assertEquals(AccountType.LIABILITY, account.type());
        assertEquals("GBP", account.currency());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Account("  ", AccountType.LIABILITY, "GBP"));
    }

    @Test
    void rejectsNullType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Account("customer:alice:gbp", null, "GBP"));
    }

    @Test
    void rejectsNullCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> new Account("customer:alice:gbp", AccountType.LIABILITY, null));
    }

    @Test
    void rejectsWrongLengthCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> new Account("customer:alice:gbp", AccountType.LIABILITY, "GB"));
    }

    @Test
    void rejectsLowercaseCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> new Account("customer:alice:gbp", AccountType.LIABILITY, "gbp"));
    }
}