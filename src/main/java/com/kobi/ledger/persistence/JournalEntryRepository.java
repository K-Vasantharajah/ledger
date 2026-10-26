package com.kobi.ledger.persistence;

import com.kobi.ledger.domain.Direction;
import com.kobi.ledger.domain.JournalEntry;
import com.kobi.ledger.domain.Posting;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JournalEntryRepository {

    private final JdbcClient jdbc;

    public JournalEntryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long save(JournalEntry entry) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO journal_entries (idempotency_key, effective_date, description)
                VALUES (:key, :date, :description)
                """)
                .param("key", entry.idempotencyKey())
                .param("date", entry.effectiveDate())
                .param("description", entry.description())
                .update(keyHolder, "id");
        long entryId = keyHolder.getKey().longValue();

        for (Posting posting : entry.postings()) {
            jdbc.sql("""
                    INSERT INTO postings (journal_entry_id, account_id, direction, amount_pence)
                    VALUES (:entryId, :accountId, :direction, :amount)
                    """)
                    .param("entryId", entryId)
                    .param("accountId", posting.accountId())
                    .param("direction", posting.direction().name())
                    .param("amount", posting.amountPence())
                    .update();
        }
        return entryId;
    }

    public Optional<JournalEntry> findById(long id) {
        List<Posting> postings = jdbc.sql("""
                SELECT account_id, direction, amount_pence
                FROM postings
                WHERE journal_entry_id = :id
                ORDER BY id
                """)
                .param("id", id)
                .query((rs, rowNum) -> new Posting(
                        rs.getLong("account_id"),
                        Direction.valueOf(rs.getString("direction")),
                        rs.getLong("amount_pence")))
                .list();

        return jdbc.sql("""
                SELECT idempotency_key, effective_date, description
                FROM journal_entries
                WHERE id = :id
                """)
                .param("id", id)
                .query((rs, rowNum) -> new JournalEntry(
                        rs.getString("idempotency_key"),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getString("description"),
                        postings))
                .optional();
    }
}