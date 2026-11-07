package com.kobi.ledger.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.kobi.ledger.domain.Account;
import com.kobi.ledger.persistence.AccountRepository;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accounts;

    public AccountController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@RequestBody CreateAccountRequest request) {
        var account = new Account(request.name(), request.type(), request.currency());
        long id = accounts.create(account);
        return new IdResponse(id);
    }
}
