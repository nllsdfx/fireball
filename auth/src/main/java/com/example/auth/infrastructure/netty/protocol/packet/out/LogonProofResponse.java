package com.example.auth.infrastructure.netty.protocol.packet.out;

/**
 * CMD_AUTH_LOGON_PROOF (0x01) — server's response after verifying the client proof.
 *
 * @param error        result code (0x00 = success)
 * @param serverProof  M2 — server's proof of shared session key (20 bytes)
 * @param accountFlags account flags (0x00800000 = survey, 0x00000000 = none)
 */
public record LogonProofResponse(
        byte error,
        byte[] serverProof,
        int accountFlags
) implements AuthResponse {}
