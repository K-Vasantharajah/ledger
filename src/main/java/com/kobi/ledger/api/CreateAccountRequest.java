package com.kobi.ledger.api;

import com.kobi.ledger.domain.AccountType;

public record CreateAccountRequest(String name, AccountType type, String currency) {
}