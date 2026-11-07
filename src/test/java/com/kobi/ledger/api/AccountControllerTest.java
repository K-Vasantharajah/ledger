package com.kobi.ledger.api;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kobi.ledger.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createsAccount() throws Exception {
        mvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "customer:sam:gbp", "type": "LIABILITY", "currency": "GBP"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void returnsBadRequestForInvalidCurrency() throws Exception {
        mvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "customer:tom:gbp", "type": "LIABILITY", "currency": "gbp"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsZeroBalanceForNewAccount() throws Exception {
        var result = mvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "customer:lucy:gbp", "type": "LIABILITY", "currency": "GBP"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/accounts/" + id + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balancePence").value(0));
    }

    @Test
    void returnsNotFoundForUnknownAccount() throws Exception {
        mvc.perform(get("/accounts/999999/balance"))
                .andExpect(status().isNotFound());
    }
}
