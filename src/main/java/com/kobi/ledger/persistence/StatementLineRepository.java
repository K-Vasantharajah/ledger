package com.kobi.ledger.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.kobi.ledger.domain.StatementLine;

@Repository
public class StatementLineRepository {

    private final JdbcClient jdbc;

    public StatementLineRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean save(long accountId, StatementLine line) {
        int inserted = jdbc.sql("""
                INSERT INTO statement_lines
                    (account_id, bank_transaction_id, value_date, direction, amount_pence, reference, description)
                VALUES (:accountId, :bankTransactionId, :valueDate, :direction, :amountPence, :reference, :description)
                ON CONFLICT (bank_transaction_id) DO NOTHING
                """)
                .param("accountId", accountId)
                .param("bankTransactionId", line.bankTransactionId())
                .param("valueDate", line.valueDate())
                .param("direction", line.direction().name())
                .param("amountPence", line.amountPence())
                .param("reference", line.reference())
                .param("description", line.description())
                .update();
        return inserted == 1;
    }
}