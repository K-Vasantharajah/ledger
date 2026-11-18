package com.kobi.ledger.service;

import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import com.kobi.ledger.domain.Direction;
import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;
import com.kobi.ledger.persistence.AccountRepository;
import com.kobi.ledger.persistence.JournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerServiceRaceTest {

    @Test
    void duplicateKeyOnSaveIsTreatedAsRetry() {
        var entries = mock(JournalEntryRepository.class);
        var entry = new JournalEntry("race-key", LocalDate.of(2026, 10, 31), "Race test",
                List.of(new Posting(1L, Direction.DEBIT, 10_000),
                        new Posting(2L, Direction.CREDIT, 10_000)));

        when(entries.findIdByIdempotencyKey("race-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(42L));
        when(entries.save(entry)).thenThrow(new DuplicateKeyException("duplicate key"));
        when(entries.findById(42L)).thenReturn(Optional.of(entry));

        var accounts = mock(AccountRepository.class);
        when(accounts.findById(1L)).thenReturn(Optional.of(new Account("asset:race:one", AccountType.ASSET, "GBP")));
        when(accounts.findById(2L)).thenReturn(Optional.of(new Account("asset:race:two", AccountType.ASSET, "GBP")));

        var ledger = new LedgerService(entries, accounts,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));

        assertEquals(42L, ledger.post(entry));
    }

    @Test
    void duplicateKeyWithDifferentEntryIsAConflict() {
        var entries = mock(JournalEntryRepository.class);
        var entry = new JournalEntry("race-key", LocalDate.of(2026, 10, 31), "Race test",
                List.of(new Posting(1L, Direction.DEBIT, 10_000),
                        new Posting(2L, Direction.CREDIT, 10_000)));
        var winnersEntry = new JournalEntry("race-key", LocalDate.of(2026, 10, 31), "Race test",
                List.of(new Posting(1L, Direction.DEBIT, 5_000),
                        new Posting(2L, Direction.CREDIT, 5_000)));

        when(entries.findIdByIdempotencyKey("race-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(42L));
        when(entries.save(entry)).thenThrow(new DuplicateKeyException("duplicate key"));
        when(entries.findById(42L)).thenReturn(Optional.of(winnersEntry));

        var accounts = mock(AccountRepository.class);
        when(accounts.findById(1L)).thenReturn(Optional.of(new Account("asset:race:one", AccountType.ASSET, "GBP")));
        when(accounts.findById(2L)).thenReturn(Optional.of(new Account("asset:race:two", AccountType.ASSET, "GBP")));

        var ledger = new LedgerService(entries, accounts,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));

        assertThrows(IdempotencyConflictException.class, () -> ledger.post(entry));
    }
}