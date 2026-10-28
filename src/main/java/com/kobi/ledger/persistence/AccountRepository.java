package com.kobi.ledger.persistence;

import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    public Optional<Account> findById(long id) {
        return jdbc.sql("""
                SELECT name, type, currency
                FROM accounts
                WHERE id = :id
                """)
                .param("id", id)
                .query((rs, rowNum) -> new Account(
                        rs.getString("name"),
                        AccountType.valueOf(rs.getString("type")),
                        rs.getString("currency")))
                .optional();
    }

    public Optional<Long> balance(long accountId) {
        return jdbc.sql("""
                SELECT a.type,
                       COALESCE(SUM(p.amount_pence) FILTER (WHERE p.direction = 'DEBIT'), 0)  AS debits,
                       COALESCE(SUM(p.amount_pence) FILTER (WHERE p.direction = 'CREDIT'), 0) AS credits
                FROM accounts a
                LEFT JOIN postings p ON p.account_id = a.id
                WHERE a.id = :id
                GROUP BY a.type
                """)
                .param("id", accountId)
                .query((rs, rowNum) -> AccountType.valueOf(rs.getString("type"))
                        .balance(rs.getLong("debits"), rs.getLong("credits")))
                .optional();
    }
}