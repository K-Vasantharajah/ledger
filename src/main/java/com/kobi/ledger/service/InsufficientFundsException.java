package com.kobi.ledger.service;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(long accountId, long projectedBalance) {
        super("Account " + accountId + " would go to " + projectedBalance + " pence");
    }

}