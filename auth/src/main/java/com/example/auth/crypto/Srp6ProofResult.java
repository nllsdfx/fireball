package com.example.auth.crypto;

/**
 * Result of a successful SRP6 proof verification.
 *
 * @param serverProof M2 = SHA1(A | M1 | K) — sent to client as proof of server knowledge (20 bytes)
 * @param sessionKey  K — interleaved-SHA1 session key (40 bytes); stored in auth.account.session_key
 */
public record Srp6ProofResult(
        byte[] serverProof,
        byte[] sessionKey
) {}
