package com.example.auth.infrastructure.netty.protocol.packet.out;

/**
 * CMD_AUTH_LOGON_CHALLENGE (0x00) — server's response to the client's challenge.
 *
 * @param error         result code (0x00 = success)
 * @param serverKey     B — server's ephemeral public key (32 bytes)
 * @param generator     g — SRP6 generator (usually 7, 1 byte)
 * @param safePrime     N — SRP6 safe prime (32 bytes)
 * @param salt          s — account salt (32 bytes)
 * @param crcSalt       random bytes for client executable integrity check (16 bytes)
 * @param securityFlags 2FA flags (0x00 in 1.12)
 */
public record LogonChallengeResponse(
        byte error,
        byte[] serverKey,
        byte generator,
        byte[] safePrime,
        byte[] salt,
        byte[] crcSalt,
        byte securityFlags
) implements AuthResponse {}
