package com.kobi.ledger.api;

import java.time.LocalDate;

public record ReverseEntryRequest(LocalDate effectiveDate) {
}
