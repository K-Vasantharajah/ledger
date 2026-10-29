package com.kobi.ledger.service;

import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.persistence.JournalEntryRepository;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    private final JournalEntryRepository entries;

    public LedgerService(JournalEntryRepository entries) {
        this.entries = entries;
    }

    public long post(JournalEntry entry) {
        var foundId = entries.findIdByIdempotencyKey(entry.idempotencyKey());

        if (foundId.isEmpty()) {
            return entries.save(entry);
        }

        var existingEntry = entries.findById(foundId.get()).orElseThrow();

        if (existingEntry.equals(entry)) {
            return foundId.get();
        }

        throw new IdempotencyConflictException(entry.idempotencyKey());
    }
}