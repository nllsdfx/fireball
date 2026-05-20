package com.example.world.service;

import com.example.world.persistence.repository.WorldAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorldAccountServiceImpl implements WorldAccountService {

    private final WorldAccountRepository accountRepository;

    @Override
    public byte[] findSessionKey(String username) {
        return accountRepository.findSessionKeyByUsername(username);
    }
}
