package com.kobi.ledger.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kobi.ledger.TestcontainersConfiguration;
import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import com.kobi.ledger.domain.Direction;
import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;
import com.kobi.ledger.persistence.AccountRepository;
import com.kobi.ledger.persistence.JournalEntryRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LedgerServiceTest {

    @Autowired
    LedgerService ledger;
    @Autowired
    AccountRepository accounts;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    JournalEntryRepository entries;
    @Autowired
    AccountRepository repository;

    @Test
    void postsNewEntry() {
        long ivy = accounts.create(new Account("customer:ivy:gbp", AccountType.LIABILITY, "GBP"));
        long jack = accounts.create(new Account("customer:jack:gbp", AccountType.LIABILITY, "GBP"));

        var entry = new JournalEntry("service-new-test", LocalDate.of(2026, 10, 29), "Ivy pays Jack £100",
                List.of(new Posting(ivy, Direction.DEBIT, 10_000),
                        new Posting(jack, Direction.CREDIT, 10_000)));

        long id = ledger.post(entry);

        assertEquals(entry, entries.findById(id).orElseThrow());
    }

    @Test
    void retryReturnsOriginalIdWithoutPostingTwice() {
        long kate = accounts.create(new Account("customer:kate:gbp", AccountType.LIABILITY, "GBP"));
        long leo = accounts.create(new Account("customer:leo:gbp", AccountType.LIABILITY, "GBP"));

        var entry = new JournalEntry("service-retry-test", LocalDate.of(2026, 10, 30), "Kate pays Leo £100",
                List.of(new Posting(kate, Direction.DEBIT, 10_000),
                        new Posting(leo, Direction.CREDIT, 10_000)));
        long firstId = ledger.post(entry);

        var sameEntry = new JournalEntry("service-retry-test", LocalDate.of(2026, 10, 30), "Kate pays Leo £100",
                List.of(new Posting(kate, Direction.DEBIT, 10_000),
                        new Posting(leo, Direction.CREDIT, 10_000)));
        long secondId = ledger.post(sameEntry);

        assertEquals(firstId, secondId);
        long postings = jdbc.sql("SELECT COUNT(*) FROM postings WHERE journal_entry_id = :id")
                .param("id", firstId)
                .query(Long.class)
                .single();

        assertEquals(2, postings);
    }

    @Test
    void rejectsDifferentEntryWithSameKey() {
        long mia = accounts.create(new Account("customer:mia:gbp", AccountType.LIABILITY, "GBP"));
        long noah = accounts.create(new Account("customer:noah:gbp", AccountType.LIABILITY, "GBP"));

        var entry = new JournalEntry("service-conflict-test", LocalDate.of(2026, 10, 31), "Mia pays Noah £100",
                List.of(new Posting(mia, Direction.DEBIT, 10_000),
                        new Posting(noah, Direction.CREDIT, 10_000)));
        ledger.post(entry);

        var conflictingEntry = new JournalEntry("service-conflict-test", LocalDate.of(2026, 10, 31),
                "Mia pays Noah £100",
                List.of(new Posting(mia, Direction.DEBIT, 5_000),
                        new Posting(noah, Direction.CREDIT, 5_000)));

        assertThrows(IdempotencyConflictException.class, () -> ledger.post(conflictingEntry));
    }
}