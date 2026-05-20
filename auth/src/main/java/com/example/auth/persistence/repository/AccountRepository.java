package com.example.auth.persistence.repository;

import com.example.auth.persistence.generated.tables.Account;
import com.example.auth.persistence.generated.tables.records.AccountRecord;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final DSLContext dslContext;

    public Optional<AccountRecord> findByUsername(@NonNull String username) {
        return dslContext.selectFrom(Account.ACCOUNT)
                .where(Account.ACCOUNT.USERNAME.eq(username))
                .fetchOptional();
    }

    public void saveSessionKey(@NonNull String username, byte[] sessionKey) {
        dslContext.update(Account.ACCOUNT)
                .set(Account.ACCOUNT.SESSION_KEY, sessionKey)
                .where(Account.ACCOUNT.USERNAME.eq(username))
                .execute();
    }
}
