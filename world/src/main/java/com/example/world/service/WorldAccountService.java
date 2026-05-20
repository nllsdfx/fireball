package com.example.world.service;

public interface WorldAccountService {

    /**
     * Returns the 40-byte session key for the given account, or null if not found.
     */
    byte[] findSessionKey(String username);
}
