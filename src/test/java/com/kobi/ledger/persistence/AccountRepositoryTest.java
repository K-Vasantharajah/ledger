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
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountRepositoryTest {

    @Autowired
    AccountRepository repository;

    @Test
    void savesAndReadsBackAccount() {
        var account = new Account("customer:alice:gbp", AccountType.LIABILITY, "GBP");

        long id = repository.create(account);
        var found = repository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(account, found.get());
    }

    @Test
    void returnsEmptyForUnknownId() {
        var found = repository.findById(999_999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void rejectsDuplicateAccountName() {
        var account = new Account("customer:bob:gbp", AccountType.LIABILITY, "GBP");
        repository.create(account);

        assertThrows(DuplicateKeyException.class, () -> repository.create(account));
    }

    @Autowired
    JournalEntryRepository entries;

    @Test
    void calculatesBalancesAfterTopUpAndPayment() {
        long bank = repository.create(new Account("asset:bank:balance-test", AccountType.ASSET, "GBP"));
        long alice = repository.create(new Account("customer:alice:balance-test", AccountType.LIABILITY, "GBP"));
        long bob = repository.create(new Account("customer:bob:balance-test", AccountType.LIABILITY, "GBP"));
        long fees = repository.create(new Account("revenue:fees:balance-test", AccountType.REVENUE, "GBP"));

        entries.save(new JournalEntry("balance-test-top-up", LocalDate.of(2026, 10, 28), "Alice tops up £100",
                List.of(new Posting(bank, Direction.DEBIT, 10_000),
                        new Posting(alice, Direction.CREDIT, 10_000))));

        entries.save(new JournalEntry("balance-test-payment", LocalDate.of(2026, 10, 28), "Alice pays Bob £30",
                List.of(new Posting(alice, Direction.DEBIT, 3_050),
                        new Posting(bob, Direction.CREDIT, 3_000),
                        new Posting(fees, Direction.CREDIT, 50))));

        assertEquals(10_000, repository.balance(bank).orElseThrow());
        assertEquals(6_950, repository.balance(alice).orElseThrow());
        assertEquals(3_000, repository.balance(bob).orElseThrow());
        assertEquals(50, repository.balance(fees).orElseThrow());
    }

}