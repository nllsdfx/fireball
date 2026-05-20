package com.example.auth.service;

import com.example.auth.persistence.generated.tables.records.AccountRecord;
import com.example.auth.persistence.repository.AccountRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @NonNull
    @Override
    public AccountRecord findAccountByUserName(@NonNull String username) {
        return accountRepository
                .findByUsername(username)
                .orElseThrow(() -> new AuthProtocolException(AuthErrorCode.UNKNOWN_ACCOUNT, "No account: " + username));
    }

    @Override
    public void saveSessionKey(@NonNull String username, byte[] sessionKey) {
        accountRepository.saveSessionKey(username, sessionKey);
    }
}
