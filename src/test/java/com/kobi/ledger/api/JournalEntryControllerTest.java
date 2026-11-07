package com.kobi.ledger.api;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.kobi.ledger.TestcontainersConfiguration;
import com.kobi.ledger.domain.Account;
import com.kobi.ledger.domain.AccountType;
import com.kobi.ledger.persistence.AccountRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JournalEntryControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accounts;

    private static String entryJson(long debitAccount, long creditAccount, long amountPence) {
        return """
                {
                  "effectiveDate": "2026-11-07",
                  "description": "Test payment",
                  "postings": [
                    { "accountId": %d, "direction": "DEBIT",  "amountPence": %d },
                    { "accountId": %d, "direction": "CREDIT", "amountPence": %d }
                  ]
                }
                """.formatted(debitAccount, amountPence, creditAccount, amountPence);
    }

    @Test
    void postsEntryAndReturnsCreated() throws Exception {
        long sara = accounts.create(new Account("customer:sara:gbp", AccountType.LIABILITY, "GBP"));
        long tom = accounts.create(new Account("customer:tom:gbp", AccountType.LIABILITY, "GBP"));

        mvc.perform(post("/journal-entries")
                .header("Idempotency-Key", "api-new-entry-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entryJson(sara, tom, 10_000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void retryWithSameKeyReturnsSameId() throws Exception {
        long kim = accounts.create(new Account("customer:kim:gbp", AccountType.LIABILITY, "GBP"));
        long liam = accounts.create(new Account("customer:liam:gbp", AccountType.LIABILITY, "GBP"));

        var result1 = mvc.perform(post("/journal-entries")
                .header("Idempotency-Key", "api-retry-entry-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entryJson(kim, liam, 10_000)))
                .andExpect(status().isCreated())
                .andReturn();

        var result2 = mvc.perform(post("/journal-entries")
                .header("Idempotency-Key", "api-retry-entry-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entryJson(kim, liam, 10_000)))
                .andExpect(status().isCreated())
                .andReturn();

        long id1 = ((Number) JsonPath.read(result1.getResponse().getContentAsString(), "$.id"))
                .longValue();
        long id2 = ((Number) JsonPath.read(result2.getResponse().getContentAsString(), "$.id"))
                .longValue();

        assertEquals(id1, id2);
    }

    @Test
    void returnsConflictForSameKeyWithDifferentEntry() throws Exception {
        long nina = accounts.create(new Account("customer:nina:gbp", AccountType.LIABILITY, "GBP"));
        long jake = accounts.create(new Account("customer:jake:gbp", AccountType.LIABILITY, "GBP"));

        mvc.perform(post("/journal-entries")
                .header("Idempotency-Key", "api-conflict-entry-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entryJson(nina, jake, 10_000)))
                .andExpect(status().isCreated());

        mvc.perform(post("/journal-entries")
                .header("Idempotency-Key", "api-conflict-entry-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entryJson(jake, nina, 10_000)))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsBadRequestForUnbalancedEntry() throws Exception {
        long george = accounts.create(new Account("customer:george:gbp", AccountType.LIABILITY, "GBP"));
        long helen = accounts.create(new Account("customer:helen:gbp", AccountType.LIABILITY, "GBP"));

        var body = """
                {
                  "effectiveDate": "2026-11-07",
                  "description": "Unbalanced payment",
                  "postings": [
                    { "accountId": %d, "direction": "DEBIT",  "amountPence": 10000 },
                    { "accountId": %d, "direction": "CREDIT", "amountPence": 9000 }
                  ]
                }
                """.formatted(george, helen);

        mvc.perform(post("/journal-entries")
                .header("Idempotency-Key", "api-unbalanced-entry-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Unbalanced")));
    }

    @Test
    void returnsBadRequestWhenIdempotencyKeyMissing() throws Exception {
        long ian = accounts.create(new Account("customer:ian:gbp", AccountType.LIABILITY, "GBP"));
        long jill = accounts.create(new Account("customer:jill:gbp", AccountType.LIABILITY, "GBP"));

        mvc.perform(post("/journal-entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entryJson(ian, jill, 10_000)))
                .andExpect(status().isBadRequest());
    }
}
