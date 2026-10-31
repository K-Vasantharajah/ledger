package com.kobi.ledger.service;

import com.kobi.ledger.domain.Direction;
import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;
import com.kobi.ledger.persistence.JournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

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

        var ledger = new LedgerService(entries);

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

        var ledger = new LedgerService(entries);

        assertThrows(IdempotencyConflictException.class, () -> ledger.post(entry));
    }
}