package com.kobi.ledger.api;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
