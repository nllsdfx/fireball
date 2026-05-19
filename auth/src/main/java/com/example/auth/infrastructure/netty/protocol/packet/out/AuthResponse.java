package com.example.auth.infrastructure.netty.protocol.packet.out;

public sealed interface AuthResponse
        permits LogonChallengeResponse, LogonProofResponse, RealmListResponse {}
