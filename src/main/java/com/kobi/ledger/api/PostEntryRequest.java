package com.kobi.ledger.api;

import java.time.LocalDate;
import java.util.List;

public record PostEntryRequest(LocalDate effectiveDate, String description, List<PostingRequest> postings) {
}
