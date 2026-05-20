package com.example.auth.crypto;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

/**
 * WoW 1.12 SRP6 implementation, ported from cmangos SRP6.cpp.
 *
 * Byte order: wire format is little-endian (LE). Java BigInteger is big-endian (BE).
 * fromWire() and toWire() handle the conversion.
 * SHA1 inputs match cmangos AsByteArray(minSize=0, reverse=true): minimum bytes, LE.
 */
@Service
public class Srp6Service {

    // WoW 1.12 SRP6 group parameters — fixed constants shared by all 1.12 clients
    private static final BigInteger N = new BigInteger(
            "894B645E89E1535BBDAD5B8B290650530801B18EBFBF5E8FAB3C82872A3E9BB7", 16);
    private static final BigInteger G          = BigInteger.valueOf(7);
    private static final BigInteger MULTIPLIER = BigInteger.valueOf(3); // k=3 (WoW variant, not SRP-6a H(N,g))

    private static final int KEY_SIZE         = 32;
    private static final int SHA1_SIZE        = 20;
    private static final int SESSION_KEY_SIZE = 40;
    private static final int B_RANDOM_BITS    = 19 * 8; // 152-bit server ephemeral private key
    private static final int CRC_SALT_SIZE    = 16;

    // Wire-format constants sent to client in CMD_AUTH_LOGON_CHALLENGE response
    public static final byte[] SAFE_PRIME_LE = toWire(N, KEY_SIZE);
    public static final byte   GENERATOR     = (byte) G.intValue();

    // SHA1(N) XOR SHA1(g) — constant term in M1; precomputed once at class load
    private static final byte[] XOR_NG;
    static {
        byte[] hashN = sha1(SAFE_PRIME_LE);
        byte[] hashG = sha1(new byte[]{GENERATOR});
        XOR_NG = new byte[SHA1_SIZE];
        for (int i = 0; i < SHA1_SIZE; i++) XOR_NG[i] = (byte) (hashN[i] ^ hashG[i]);
    }

    private final SecureRandom rng = new SecureRandom();

    /**
     * Generates server ephemeral key pair and challenge state from the account's DB record.
     *
     * @param salt     32-byte LE salt from auth.account.salt
     * @param verifier 32-byte LE verifier from auth.account.verifier
     */
    public Srp6Challenge beginChallenge(byte[] salt, byte[] verifier) {
        BigInteger v = fromWire(verifier);
        BigInteger b = new BigInteger(B_RANDOM_BITS, rng);
        BigInteger B = MULTIPLIER.multiply(v).add(G.modPow(b, N)).mod(N);
        return new Srp6Challenge(b, B, v, salt);
    }

    /** Generates 16 random bytes for the WoW client exe integrity check field. */
    public byte[] generateCrcSalt() {
        byte[] salt = new byte[CRC_SALT_SIZE];
        rng.nextBytes(salt);
        return salt;
    }

    /**
     * Verifies the client's M1 proof and returns M2 + session key on success.
     *
     * @param challenge           state from {@link #beginChallenge}
     * @param username            account name (uppercase ASCII)
     * @param clientPublicKeyWire A — 32-byte LE from wire
     * @param m1                  client proof — 20 bytes from wire
     * @return proof result, or empty if M1 verification failed
     */
    public Optional<Srp6ProofResult> verifyProof(
            Srp6Challenge challenge, String username,
            byte[] clientPublicKeyWire, byte[] m1) {

        BigInteger A = fromWire(clientPublicKeyWire);
        if (A.mod(N).equals(BigInteger.ZERO)) return Optional.empty();

        byte[] aWire = toWire(A);                           // minimum bytes LE — mirrors cmangos AsByteArray()
        byte[] bWire = toWire(challenge.serverPublicKey()); // minimum bytes LE

        // u = SHA1(A | B), interpreted as LE BigInteger
        BigInteger u = fromWire(sha1(aWire, bWire));

        // S = (A · v^u)^b mod N
        BigInteger S = A.multiply(challenge.verifier().modPow(u, N))
                        .modPow(challenge.serverPrivateKey(), N);

        // S padded to 32 bytes — mirrors cmangos AsByteArray(32) in HashSessionKey
        byte[] K          = hashSessionKey(toWire(S, KEY_SIZE));
        byte[] expectedM1 = computeM1(username, challenge.salt(), aWire, bWire, K);

        if (!MessageDigest.isEqual(expectedM1, m1)) return Optional.empty();

        byte[] m2 = sha1(aWire, expectedM1, K);
        return Optional.of(new Srp6ProofResult(m2, K));
    }

    /**
     * M1 = SHA1(SHA1(N) XOR SHA1(g) | SHA1(username) | s | A | B | K)
     * Mirrors cmangos SRP6::CalculateProof — the BigNumber t3 SetBinary/AsByteArray round-trip
     * is a no-op on byte order (SetBinary reverses, AsByteArray reverses back).
     */
    private static byte[] computeM1(String username, byte[] salt, byte[] aWire, byte[] bWire, byte[] K) {
        return sha1(XOR_NG, sha1(username.getBytes(StandardCharsets.US_ASCII)), salt, aWire, bWire, K);
    }

    /**
     * WoW-specific session key derivation: interleaved SHA1 of even and odd bytes of S.
     * Mirrors cmangos SRP6::HashSessionKey.
     *
     * @param s32 S as exactly 32 bytes LE
     */
    private static byte[] hashSessionKey(byte[] s32) {
        byte[] even = new byte[16];
        byte[] odd  = new byte[16];
        for (int i = 0; i < 16; i++) {
            even[i] = s32[i * 2];
            odd[i]  = s32[i * 2 + 1];
        }
        byte[] hashEven = sha1(even);
        byte[] hashOdd  = sha1(odd);
        byte[] result   = new byte[SESSION_KEY_SIZE];
        for (int i = 0; i < SHA1_SIZE; i++) {
            result[i * 2]     = hashEven[i];
            result[i * 2 + 1] = hashOdd[i];
        }
        return result;
    }

    /** LE wire bytes → unsigned BigInteger (mirrors BigNumber::SetBinary). */
    private static BigInteger fromWire(byte[] le) {
        byte[] be = Arrays.copyOf(le, le.length);
        reverse(be);
        return new BigInteger(1, be);
    }

    /**
     * BigInteger → minimum-length LE byte array.
     * Mirrors BigNumber::AsByteArray(minSize=0, reverse=true): minimum bytes, little-endian.
     */
    public static byte[] toWire(BigInteger n) {
        byte[] be  = n.toByteArray();
        int start  = (be[0] == 0) ? 1 : 0; // strip two's-complement sign byte
        int len    = be.length - start;
        byte[] le  = new byte[len];
        for (int i = 0; i < len; i++) le[i] = be[start + len - 1 - i];
        return le;
    }

    /**
     * BigInteger → exactly {@code length} bytes LE (zero-padded at the high end).
     * Mirrors BigNumber::AsByteArray(minSize=length, reverse=true).
     */
    public static byte[] toWire(BigInteger n, int length) {
        byte[] be      = n.toByteArray();
        int start      = (be[0] == 0) ? 1 : 0;
        int dataLen    = be.length - start;
        if (dataLen > length) throw new IllegalArgumentException(
                "Value requires %d bytes but wire format allows %d".formatted(dataLen, length));
        byte[] le = new byte[length];
        for (int i = 0; i < dataLen; i++) le[i] = be[start + dataLen - 1 - i];
        return le;
    }

    private static void reverse(byte[] arr) {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            byte tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
    }

    private static byte[] sha1(byte[]... inputs) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (byte[] input : inputs) md.update(input);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
