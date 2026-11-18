package com.kobi.ledger.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
class LedgerServiceConcurrencyTest {

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
    void concurrentPaymentsNeverOverdraw() throws Exception {
        long payer = accounts.create(new Account("service-race-overdraw:payer", AccountType.LIABILITY, "GBP"));
        long payee = accounts.create(new Account("service-race-overdraw:payee", AccountType.LIABILITY, "GBP"));
        fund(payer, 10_000, "service-race-overdraw-topup");

        int attempts = 10;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);
        var results = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < attempts; i++) {
            var payment = new JournalEntry("service-race-overdraw-" + i, LocalDate.of(2026, 11, 15),
                    "Payer pays £30, attempt " + i,
                    List.of(new Posting(payer, Direction.DEBIT, 3_000),
                            new Posting(payee, Direction.CREDIT, 3_000)));

            results.add(pool.submit(() -> {
                start.await(); // every thread waits at this line...
                try {
                    ledger.post(payment);
                    return true;
                } catch (InsufficientFundsException e) {
                    return false;
                }
            }));
        }

        start.countDown(); // ...until this releases them all at once
        int succeeded = 0;
        for (var result : results) {
            if (result.get())
                succeeded++; // get() waits for each thread to finish
        }
        pool.shutdown();

        long balance = accounts.balance(payer).orElseThrow();
        assertTrue(balance >= 0, "Payer overdrawn: " + balance);
        assertEquals(10_000 - succeeded * 3_000, balance);
    }

    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        long alice = accounts.create(new Account("service-race-deadlock:alice", AccountType.LIABILITY, "GBP"));
        long bob = accounts.create(new Account("service-race-deadlock:bob", AccountType.LIABILITY, "GBP"));
        fund(alice, 100_000, "service-race-deadlock-topup-alice");
        fund(bob, 100_000, "service-race-deadlock-topup-bob");

        int attempts = 20;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);
        var results = new ArrayList<Future<Long>>();

        for (int i = 0; i < attempts; i++) {
            long from = (i % 2 == 0) ? alice : bob;
            long to = (i % 2 == 0) ? bob : alice;

            var payment = new JournalEntry("service-race-deadlock-" + i, LocalDate.of(2026, 11, 15),
                    "Transfer attempt " + i,
                    List.of(new Posting(from, Direction.DEBIT, 1_000),
                            new Posting(to, Direction.CREDIT, 1_000)));

            results.add(pool.submit(() -> {
                start.await();
                return ledger.post(payment); // returns the entry id; any exception fails the test via get()
            }));
        }

        start.countDown(); // release all threads at once
        for (var result : results) {
            result.get(); // // re-throws any exception from the task
        }
        pool.shutdown();

        long balanceAlice = accounts.balance(alice).orElseThrow();
        long balanceBob = accounts.balance(bob).orElseThrow();
        assertEquals(100_000, balanceAlice);
        assertEquals(100_000, balanceBob);
    }
}