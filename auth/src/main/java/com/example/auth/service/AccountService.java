package com.example.auth.service;

import com.example.auth.persistence.generated.tables.records.AccountRecord;
import lombok.NonNull;

public interface AccountService {

    @NonNull
    AccountRecord findAccountByUserName(@NonNull String username);

    void saveSessionKey(@NonNull String username, byte[] sessionKey);
}
