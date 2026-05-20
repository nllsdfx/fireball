package com.example.auth.crypto;

import java.math.BigInteger;

/**
 * Transient per-connection state held between CMD_AUTH_LOGON_CHALLENGE and CMD_AUTH_LOGON_PROOF.
 *
 * @param serverPrivateKey b — server ephemeral private key (19 random bytes)
 * @param serverPublicKey  B = (k·v + g^b) mod N (32 bytes wire)
 * @param verifier         v = g^x mod N loaded from DB
 * @param salt             s — per-account random salt (32 bytes LE, as stored in DB)
 */
public record Srp6Challenge(
        BigInteger serverPrivateKey,
        BigInteger serverPublicKey,
        BigInteger verifier,
        byte[] salt
) {}
