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

    private void fund(long customer, long amountPence, String key) {
        long bank = accounts.create(new Account(key + ":bank", AccountType.ASSET, "GBP"));
        ledger.post(new JournalEntry(key, LocalDate.of(2026, 11, 15), "Top-up",
                List.of(new Posting(bank, Direction.DEBIT, amountPence),
                        new Posting(customer, Direction.CREDIT, amountPence))));
    }

    @Test
    void postsNewEntry() {
        long ivy = accounts.create(new Account("customer:ivy:gbp", AccountType.LIABILITY, "GBP"));
        long jack = accounts.create(new Account("customer:jack:gbp", AccountType.LIABILITY, "GBP"));
        fund(ivy, 10_000, "service-new-test-topup");

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
        fund(kate, 10_000, "service-retry-test-topup");

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
        fund(mia, 10_000, "service-conflict-test-topup");

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

    @Test
    void reversalRestoresBalancesFromBeforePayment() {
        long olivia = accounts.create(new Account("customer:olivia:gbp", AccountType.LIABILITY, "GBP"));
        long paul = accounts.create(new Account("customer:paul:gbp", AccountType.LIABILITY, "GBP"));
        fund(olivia, 10_000, "service-restore-balances-test-topup");

        assertEquals(10_000, accounts.balance(olivia).orElseThrow());
        assertEquals(0, accounts.balance(paul).orElseThrow());

        var payment = new JournalEntry("service-restore-balances-test", LocalDate.of(2026, 11, 5),
                "Olivia pays Paul £100",
                List.of(new Posting(olivia, Direction.DEBIT, 10_000),
                        new Posting(paul, Direction.CREDIT, 10_000)));
        long paymentId = ledger.post(payment);

        assertEquals(0, accounts.balance(olivia).orElseThrow());
        assertEquals(10_000, accounts.balance(paul).orElseThrow());

        ledger.reverse(paymentId, "service-restore-balances-test-undo", LocalDate.of(2026, 11, 5));

        assertEquals(10_000, accounts.balance(olivia).orElseThrow());
        assertEquals(0, accounts.balance(paul).orElseThrow());
    }

    @Test
    void rejectsReversingSameEntryTwice() {
        long quinn = accounts.create(new Account("customer:quinn:gbp", AccountType.LIABILITY, "GBP"));
        long ryan = accounts.create(new Account("customer:ryan:gbp", AccountType.LIABILITY, "GBP"));
        fund(quinn, 10_000, "service-reversal-twice-test-topup");

        var payment = new JournalEntry("service-reversal-twice-test", LocalDate.of(2026, 11, 6), "Quinn pays Ryan £100",
                List.of(new Posting(quinn, Direction.DEBIT, 10_000),
                        new Posting(ryan, Direction.CREDIT, 10_000)));
        long paymentId = ledger.post(payment);

        ledger.reverse(paymentId, "service-reversal-twice-test-undo", LocalDate.of(2026, 11, 6));

        assertThrows(AlreadyReversedException.class,
                () -> ledger.reverse(paymentId, "service-reversal-twice-test-undo-again", LocalDate.of(2026, 11, 6)));
    }

    @Test
    void rejectsReversingUnknownEntry() {
        assertThrows(EntryNotFoundException.class,
                () -> ledger.reverse(999_999L, "service-reversal-unknown-test", LocalDate.of(2026, 11, 7)));
    }

    @Test
    void retryingSameReversalReturnsSameId() {
        long sophia = accounts.create(new Account("customer:sophia:gbp", AccountType.LIABILITY, "GBP"));
        long theo = accounts.create(new Account("customer:theo:gbp", AccountType.LIABILITY, "GBP"));
        fund(sophia, 10_000, "service-reversal-retry-test-topup");

        var payment = new JournalEntry("service-reversal-retry-test", LocalDate.of(2026, 11, 9),
                "Sophia pays Theo £100",
                List.of(new Posting(sophia, Direction.DEBIT, 10_000),
                        new Posting(theo, Direction.CREDIT, 10_000)));
        long paymentId = ledger.post(payment);

        long firstId = ledger.reverse(paymentId, "service-reversal-retry-test-undo", LocalDate.of(2026, 11, 9));
        long retryId = ledger.reverse(paymentId, "service-reversal-retry-test-undo", LocalDate.of(2026, 11, 9));

        assertEquals(firstId, retryId);
    }

    @Test
    void rejectsPaymentThatWouldOverdraw() {
        long uma = accounts.create(new Account("customer:uma:gbp", AccountType.LIABILITY, "GBP"));
        long victor = accounts.create(new Account("customer:victor:gbp", AccountType.LIABILITY, "GBP"));
        fund(uma, 5_000, "service-overdraw-test-topup");

        var payment = new JournalEntry("service-overdraw-test", LocalDate.of(2026, 11, 10),
                "Uma pays Victor £100",
                List.of(new Posting(uma, Direction.DEBIT, 10_000),
                        new Posting(victor, Direction.CREDIT, 10_000)));

        assertThrows(InsufficientFundsException.class, () -> ledger.post(payment));
        assertEquals(5_000, accounts.balance(uma).orElseThrow());
        assertTrue(entries.findIdByIdempotencyKey("service-overdraw-test").isEmpty());
    }

    @Test
    void allowsPaymentDownToExactlyZero() {
        long wendy = accounts.create(new Account("customer:wendy:gbp", AccountType.LIABILITY, "GBP"));
        long xavier = accounts.create(new Account("customer:xavier:gbp", AccountType.LIABILITY, "GBP"));
        fund(wendy, 10_000, "service-exact-zero-test-topup");

        var payment = new JournalEntry("service-exact-zero-test", LocalDate.of(2026, 11, 11),
                "Wendy pays Xavier £100",
                List.of(new Posting(wendy, Direction.DEBIT, 10_000),
                        new Posting(xavier, Direction.CREDIT, 10_000)));

        assertDoesNotThrow(() -> ledger.post(payment));
        assertEquals(0, accounts.balance(wendy).orElseThrow());
        assertEquals(10_000, accounts.balance(xavier).orElseThrow());
    }

    @Test
    void checksCombinedEffectOfPostingsOnSameAccount() {
        long yasmine = accounts.create(new Account("customer:yasmine:gbp", AccountType.LIABILITY, "GBP"));
        long zane = accounts.create(new Account("customer:zane:gbp", AccountType.LIABILITY, "GBP"));
        fund(yasmine, 10_000, "service-combined-postings-test-topup");

        var payment = new JournalEntry("service-combined-postings-test", LocalDate.of(2026, 11, 12),
                "Yasmine pays Zane £110 in two parts",
                List.of(new Posting(yasmine, Direction.DEBIT, 6_000),
                        new Posting(yasmine, Direction.DEBIT, 5_000),
                        new Posting(zane, Direction.CREDIT, 11_000)));

        assertThrows(InsufficientFundsException.class, () -> ledger.post(payment));
        assertEquals(10_000, accounts.balance(yasmine).orElseThrow());
    }

    @Test
    void rejectsReversalThatWouldOverdraw() {
        long alice = accounts.create(new Account("service-reversal-overdraw:alice", AccountType.LIABILITY, "GBP"));
        long bob = accounts.create(new Account("service-reversal-overdraw:bob", AccountType.LIABILITY, "GBP"));
        fund(alice, 10_000, "service-reversal-overdraw-topup");

        var payment = new JournalEntry("service-reversal-overdraw-payment", LocalDate.of(2026, 11, 13),
                "Alice pays Bob £100",
                List.of(new Posting(alice, Direction.DEBIT, 10_000),
                        new Posting(bob, Direction.CREDIT, 10_000)));
        long paymentId = ledger.post(payment);

        var bobSpends = new JournalEntry("service-reversal-overdraw-bob-spends", LocalDate.of(2026, 11, 14),
                "Bob pays Alice £50",
                List.of(new Posting(bob, Direction.DEBIT, 5_000),
                        new Posting(alice, Direction.CREDIT, 5_000)));
        ledger.post(bobSpends);

        assertThrows(InsufficientFundsException.class,
                () -> ledger.reverse(paymentId, "service-reversal-overdraw-undo", LocalDate.of(2026, 11, 15)));

        assertEquals(5_000, accounts.balance(alice).orElseThrow());
        assertEquals(5_000, accounts.balance(bob).orElseThrow());
    }
}