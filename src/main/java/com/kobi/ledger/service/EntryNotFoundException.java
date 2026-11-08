package com.kobi.ledger.service;

public class EntryNotFoundException extends RuntimeException {
    public EntryNotFoundException(long id) {
        super("No journal entry with id " + id);
    }
}