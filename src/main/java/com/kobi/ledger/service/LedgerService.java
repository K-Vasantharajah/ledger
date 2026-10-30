package com.kobi.ledger.service;

import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.persistence.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

@Service
public class LedgerService {

    private final JournalEntryRepository entries;

    public LedgerService(JournalEntryRepository entries) {
        this.entries = entries;
    }

    public long post(JournalEntry entry) {
        var foundId = entries.findIdByIdempotencyKey(entry.idempotencyKey());
        if (foundId.isPresent()) {
            return matchExisting(foundId.get(), entry);
        }
        try {
            return entries.save(entry);
        } catch (DuplicateKeyException e) {
            long existingId = entries.findIdByIdempotencyKey(entry.idempotencyKey()).orElseThrow();
            return matchExisting(existingId, entry);
        }
    }

    private long matchExisting(long existingId, JournalEntry entry) {
        var existing = entries.findById(existingId).orElseThrow();
        if (existing.equals(entry)) {
            return existingId;
        }
        throw new IdempotencyConflictException(entry.idempotencyKey());
    }
}