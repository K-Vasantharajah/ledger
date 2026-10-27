package com.kobi.ledger.domain;

public enum AccountType {

    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE;

    public long balance(long debits, long credits) {
        return switch (this) {
            case ASSET, EXPENSE -> debits - credits;
            case LIABILITY, EQUITY, REVENUE -> credits - debits;
        };
    }
}