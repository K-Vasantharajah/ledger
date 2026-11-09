package com.kobi.ledger.service;

import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.persistence.JournalEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

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

    public long reverse(long originalId, String idempotencyKey, LocalDate effectiveDate) {
        var original = entries.findById(originalId)
                .orElseThrow(() -> new EntryNotFoundException(originalId));
        var reversal = original.reversal(idempotencyKey, effectiveDate);

        var foundId = entries.findIdByIdempotencyKey(idempotencyKey);
        if (foundId.isPresent()) {
            return matchExisting(foundId.get(), reversal);
        }
        try {
            return entries.saveReversal(reversal, originalId);
        } catch (DuplicateKeyException e) {
            var raceWinner = entries.findIdByIdempotencyKey(idempotencyKey);
            if (raceWinner.isPresent()) {
                return matchExisting(raceWinner.get(), reversal);
            }
            throw new AlreadyReversedException(originalId);
        }
    }
}