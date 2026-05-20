package com.example.auth.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class Srp6ServiceTest {

    private static final BigInteger N = new BigInteger(
            "894B645E89E1535BBDAD5B8B290650530801B18EBFBF5E8FAB3C82872A3E9BB7", 16);
    private static final BigInteger G  = BigInteger.valueOf(7);
    private static final BigInteger K3 = BigInteger.valueOf(3);

    private final Srp6Service srp6 = new Srp6Service();

    // --- toWire ---

    @Test
    void toWire_singleByte() {
        assertThat(Srp6Service.toWire(BigInteger.ONE)).isEqualTo(new byte[]{1});
    }

    @Test
    void toWire_isLittleEndian() {
        // 0x0102 → LE = [02, 01]
        assertThat(Srp6Service.toWire(new BigInteger("0102", 16)))
                .isEqualTo(new byte[]{2, 1});
    }

    @Test
    void toWire_fixedLength_zeroPadsHighBytes() {
        assertThat(Srp6Service.toWire(BigInteger.ONE, 4))
                .isEqualTo(new byte[]{1, 0, 0, 0});
    }

    @Test
    void toWire_fixedLength_throwsWhenValueExceedsLength() {
        assertThatThrownBy(() -> Srp6Service.toWire(new BigInteger("0100", 16), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- generateCrcSalt ---

    @Test
    void generateCrcSalt_is16Bytes() {
        assertThat(srp6.generateCrcSalt()).hasSize(16);
    }

    @Test
    void generateCrcSalt_producesDistinctValues() {
        assertThat(srp6.generateCrcSalt()).isNotEqualTo(srp6.generateCrcSalt());
    }

    // --- beginChallenge ---

    @Test
    void beginChallenge_preservesSalt() {
        byte[] salt = randomBytes(32);
        BigInteger v = G.modPow(BigInteger.valueOf(42), N);
        Srp6Challenge ch = srp6.beginChallenge(salt, Srp6Service.toWire(v, 32));
        assertThat(ch.salt()).isEqualTo(salt);
    }

    @Test
    void beginChallenge_serverPublicKeyIsPositive() {
        byte[] salt = new byte[32];
        BigInteger v = G.modPow(BigInteger.valueOf(42), N);
        Srp6Challenge ch = srp6.beginChallenge(salt, Srp6Service.toWire(v, 32));
        assertThat(ch.serverPublicKey().signum()).isPositive();
    }

    // --- verifyProof ---

    @Test
    void verifyProof_rejectsZeroClientKey() {
        BigInteger v = G.modPow(BigInteger.valueOf(42), N);
        Srp6Challenge ch = srp6.beginChallenge(new byte[32], Srp6Service.toWire(v, 32));

        Optional<Srp6ProofResult> result = srp6.verifyProof(ch, "ADMIN", new byte[32], new byte[20]);
        assertThat(result).isEmpty();
    }

    @Test
    void verifyProof_rejectsWrongM1() {
        BigInteger v = G.modPow(BigInteger.valueOf(12345), N);
        Srp6Challenge ch = srp6.beginChallenge(new byte[32], Srp6Service.toWire(v, 32));

        BigInteger a = new BigInteger(152, new SecureRandom());
        byte[] aWire = Srp6Service.toWire(G.modPow(a, N), 32);
        byte[] wrongM1 = new byte[20];

        assertThat(srp6.verifyProof(ch, "ADMIN", aWire, wrongM1)).isEmpty();
    }

    // --- full round-trip ---

    @Test
    void roundTrip_srp6MathIsCorrect() {
        String username = "ADMIN";
        String password = "ADMIN";
        byte[] salt = randomBytes(32);

        // Compute verifier as account registration would
        byte[] hIdentity = sha1((username + ":" + password).getBytes(StandardCharsets.US_ASCII));
        BigInteger x = fromWire(sha1(salt, hIdentity));
        BigInteger v = G.modPow(x, N);

        // Server: begin challenge
        Srp6Challenge challenge = srp6.beginChallenge(salt, Srp6Service.toWire(v, 32));
        BigInteger B = challenge.serverPublicKey();

        // Client: ephemeral key pair
        BigInteger a = new BigInteger(152, new SecureRandom());
        BigInteger A = G.modPow(a, N);

        // Both: u = fromWire(SHA1(A || B)), minimum-length LE as on the wire
        byte[] aWire = Srp6Service.toWire(A);
        byte[] bWire = Srp6Service.toWire(B);
        BigInteger u = fromWire(sha1(aWire, bWire));

        // Client: S = (B - k·v)^(a + u·x) mod N
        BigInteger S_client = B.subtract(K3.multiply(v)).mod(N).modPow(a.add(u.multiply(x)), N);
        byte[] K_client = hashSessionKey(Srp6Service.toWire(S_client, 32));

        // Client: M1
        byte[] m1 = sha1(xorNg(), sha1(username.getBytes(StandardCharsets.US_ASCII)),
                salt, aWire, bWire, K_client);

        // Server: verify
        Optional<Srp6ProofResult> result = srp6.verifyProof(
                challenge, username, Srp6Service.toWire(A, 32), m1);

        assertThat(result).isPresent();
        assertThat(result.get().sessionKey()).isEqualTo(K_client);
        // M2 = SHA1(A || M1 || K)
        assertThat(result.get().serverProof()).isEqualTo(sha1(aWire, m1, K_client));
    }

    // --- helpers ---

    private static BigInteger fromWire(byte[] le) {
        byte[] be = Arrays.copyOf(le, le.length);
        for (int i = 0, j = be.length - 1; i < j; i++, j--) {
            byte t = be[i]; be[i] = be[j]; be[j] = t;
        }
        return new BigInteger(1, be);
    }

    private static byte[] hashSessionKey(byte[] s32) {
        byte[] even = new byte[16], odd = new byte[16];
        for (int i = 0; i < 16; i++) { even[i] = s32[i * 2]; odd[i] = s32[i * 2 + 1]; }
        byte[] he = sha1(even), ho = sha1(odd), K = new byte[40];
        for (int i = 0; i < 20; i++) { K[i * 2] = he[i]; K[i * 2 + 1] = ho[i]; }
        return K;
    }

    private static byte[] xorNg() {
        byte[] hn = sha1(Srp6Service.SAFE_PRIME_LE);
        byte[] hg = sha1(new byte[]{Srp6Service.GENERATOR});
        byte[] xor = new byte[20];
        for (int i = 0; i < 20; i++) xor[i] = (byte) (hn[i] ^ hg[i]);
        return xor;
    }

    private static byte[] sha1(byte[]... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (byte[] p : parts) md.update(p);
            return md.digest();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len]; new SecureRandom().nextBytes(b); return b;
    }
}
