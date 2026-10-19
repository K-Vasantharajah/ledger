package com.kobi.ledger.domain;

public record Account(String name, AccountType type, String currency) {

    public Account {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a valid three-letter code: " + currency);
        }

    }
}