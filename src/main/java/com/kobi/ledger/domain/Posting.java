package com.kobi.ledger.domain;

public record Posting(String accountId, Direction direction, long amountPence) {
}