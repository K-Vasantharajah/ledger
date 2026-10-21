package com.kobi.ledger.persistence;

import com.kobi.ledger.TestcontainersConfiguration;
import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

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

}