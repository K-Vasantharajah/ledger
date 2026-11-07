package com.kobi.ledger.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;

import com.kobi.ledger.service.LedgerService;

@RestController
@RequestMapping("/journal-entries")
public class JournalEntryController {

    private final LedgerService ledger;

    public JournalEntryController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse post(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PostEntryRequest request) {

        var postings = request.postings().stream()
                .map(p -> new Posting(p.accountId(), p.direction(), p.amountPence()))
                .toList();
        var entry = new JournalEntry(idempotencyKey, request.effectiveDate(), request.description(), postings);

        return new IdResponse(ledger.post(entry));
    }
}
