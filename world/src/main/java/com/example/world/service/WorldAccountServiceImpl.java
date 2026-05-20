package com.example.world.service;

import com.example.world.persistence.repository.WorldAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class WorldAccountServiceImpl implements WorldAccountService {

    private final WorldAccountRepository accountRepository;

    @Override
    public byte[] findSessionKey(String username) {
        String hex = accountRepository.findSessionKeyByUsername(username);
        if (hex == null || hex.isBlank()) return null;
        return HexFormat.of().parseHex(hex);
    }
}
