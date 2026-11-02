package com.kobi.ledger.domain;

public enum Direction {
    DEBIT, CREDIT;

    public Direction opposite() {
        return switch (this) {
            case DEBIT -> CREDIT;
            case CREDIT -> DEBIT;
        };
    }
}