package com.kobi.ledger.api;

import com.kobi.ledger.domain.Direction;

public record PostingRequest(long accountId, Direction direction, long amountPence) {
}