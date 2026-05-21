package com.example.world.service;

public interface WorldAccountService {

    /**
     * Returns id + session key for the given account, or null if not found.
     */
    AccountSession findAccountSession(String username);
}