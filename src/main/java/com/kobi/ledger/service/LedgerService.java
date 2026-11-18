package com.kobi.ledger.service;

import com.kobi.ledger.domain.AccountType;
import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;
import com.kobi.ledger.persistence.AccountRepository;
import com.kobi.ledger.persistence.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;

@Service
public class LedgerService {

    private final JournalEntryRepository entries;
    private final AccountRepository accounts;
    private final TransactionTemplate tx;

    public LedgerService(JournalEntryRepository entries, AccountRepository accounts, TransactionTemplate tx) {
        this.entries = entries;
        this.accounts = accounts;
        this.tx = tx;
    }

    public long post(JournalEntry entry) {
        var foundId = entries.findIdByIdempotencyKey(entry.idempotencyKey());
        if (foundId.isPresent()) {
            return matchExisting(foundId.get(), entry);
        }
        try {
            return tx.execute(status -> {
                checkNoOverdraft(entry);
                return entries.save(entry);
            });
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

    private void checkNoOverdraft(JournalEntry entry) {
        Map<Long, Long> effects = new HashMap<>();
        for (Posting posting : entry.postings()) {
            var account = accounts.findById(posting.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("No account with id " + posting.accountId()));

            if (account.type() == AccountType.LIABILITY) {
                effects.merge(posting.accountId(), posting.effectOn(account.type()), Long::sum);
            }
        }
        var accountIds = effects.keySet().stream().sorted().toList();
        for (long id : accountIds) {
            accounts.lockForUpdate(id);
        }
        for (long id : accountIds) {
            long projected = accounts.balance(id).orElseThrow() + effects.get(id);
            if (projected < 0) {
                throw new InsufficientFundsException(id, projected);
            }
        }
    }
}