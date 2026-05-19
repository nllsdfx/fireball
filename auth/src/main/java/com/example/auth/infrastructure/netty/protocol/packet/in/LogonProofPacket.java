package com.example.auth.infrastructure.netty.protocol.packet.in;

/**
 * CMD_AUTH_LOGON_PROOF (0x01) — client's SRP6 proof sent after receiving the challenge.
 *
 * @param clientPublicKey A — client's ephemeral public key (32 bytes)
 * @param clientProof     M1 — client proof of shared session key (20 bytes)
 * @param crcHash         integrity hash of the client executable (20 bytes)
 * @param numberOfKeys    number of authenticator keys (usually 0)
 * @param securityFlags   2FA flags (usually 0 in 1.12)
 */
public record LogonProofPacket(
        byte[] clientPublicKey,
        byte[] clientProof,
        byte[] crcHash,
        byte numberOfKeys,
        byte securityFlags
) {}
