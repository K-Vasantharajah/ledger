package com.kobi.ledger.persistence;

import com.kobi.ledger.TestcontainersConfiguration;
import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import com.kobi.ledger.domain.Direction;
import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JournalEntryRepositoryTest {

    @Autowired
    JournalEntryRepository repository;
    @Autowired
    AccountRepository accounts;
    @Autowired
    JdbcClient jdbc;

    @Test
    void rollsBackWholeEntryWhenAPostingFails() {
        long bank = accounts.create(new Account("asset:bank:rollback-test", AccountType.ASSET, "GBP"));
        long missingAccount = 999_999L;

        var entry = new JournalEntry("rollback-test", LocalDate.of(2026, 10, 24), "Should not survive",
                List.of(new Posting(bank, Direction.DEBIT, 10_000), // succeeds
                        new Posting(missingAccount, Direction.CREDIT, 10_000))); // fails

        assertThrows(DataIntegrityViolationException.class, () -> repository.save(entry));

        long count = jdbc.sql("SELECT COUNT(*) FROM journal_entries WHERE idempotency_key = :key")
                .param("key", "rollback-test")
                .query(Long.class)
                .single();
        assertEquals(0, count); // the entry row must be gone too
    }

    @Test
    void savesEntryWithAllPostings() {
        long david = accounts.create(new Account("customer:david:gbp", AccountType.LIABILITY, "GBP"));
        long charlotte  = accounts.create(new Account("customer:charlotte:gbp", AccountType.LIABILITY, "GBP"));

        var entry = new JournalEntry("save-entry-test", LocalDate.of(2026, 10, 25), "Should survive",
                List.of(new Posting(david, Direction.DEBIT, 10_000),
                        new Posting(charlotte , Direction.CREDIT, 10_000)));

        long entryId = repository.save(entry);

        long count = jdbc.sql("SELECT COUNT(*) FROM postings WHERE journal_entry_id = :id")
                .param("id", entryId)
                .query(Long.class)
                .single();
        assertEquals(2, count);
    }
}