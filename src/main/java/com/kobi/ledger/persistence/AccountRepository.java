package com.kobi.ledger.persistence;

import com.kobi.ledger.domain.Account;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long create(Account account) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO accounts (name, type, currency)
                VALUES (:name, :type, :currency)
                """)
                .param("name", account.name())
                .param("type", account.type().name())
                .param("currency", account.currency())
                .update(keyHolder, "id");
        return keyHolder.getKey().longValue();
    }
}