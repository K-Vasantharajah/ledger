package com.kobi.ledger.persistence;

import com.kobi.ledger.TestcontainersConfiguration;
import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import com.kobi.ledger.domain.Direction;

import com.kobi.ledger.domain.StatementLine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StatementLineRepositoryTest {

    @Autowired
    StatementLineRepository repository;
    @Autowired
    AccountRepository accounts;
    @Autowired
    JdbcClient jdbc;

    @Test
    void savesNewLine() {
        long accountId = accounts.create(new Account("statement-new:bank", AccountType.ASSET, "GBP"));

        var line = new StatementLine("statement-new-txn", LocalDate.of(2026, 10, 24), Direction.DEBIT, 10_000, "ref",
                "desc");
        boolean wasNew = repository.save(accountId, line);

        assertTrue(wasNew);
    }

    @Test
    void ignoresDuplicateBankTransactionId() {
        long bank = accounts.create(new Account("statement-duplicate:bank", AccountType.ASSET, "GBP"));
        var line = new StatementLine("statement-duplicate-txn", LocalDate.of(2026, 11, 26), Direction.CREDIT,
                10_000, "statement-duplicate-ref", "ALICE SMITH");

        assertTrue(repository.save(bank, line));
        assertFalse(repository.save(bank, line));

        long count = jdbc.sql("SELECT COUNT(*) FROM statement_lines WHERE bank_transaction_id = :id")
                .param("id", "statement-duplicate-txn")
                .query(Long.class)
                .single();
        assertEquals(1, count);
    }
}