package com.kobi.ledger.service;

public class AlreadyReversedException extends RuntimeException {
    public AlreadyReversedException(long id) {
        super("Journal entry with id " + id + " has already been reversed");
    }
}
