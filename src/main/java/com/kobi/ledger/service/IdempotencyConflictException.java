package com.kobi.ledger.service;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Idempotency key already used for a different entry: " + key);
    }
}