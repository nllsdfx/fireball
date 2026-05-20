package com.example.world.infrastructure.netty.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldCipherTest {

    private static final byte[] ZERO_KEY = new byte[40];

    // ── round-trip ────────────────────────────────────────────────────────────
    // encryptSend (sendI/J) and decryptRecv (recvI/J) use independent state that both
    // start at 0,0 — so a single packet round-trips correctly: enc 4 bytes, dec 6
    // (2 zero padding bytes harmless), recover original 4.

    @Test
    void encryptSend_thenDecryptRecv_recoversPayload_zeroKey() {
        byte[] plain = {0x12, 0x34, 0x56, 0x78};
        WorldCipher enc = new WorldCipher(ZERO_KEY);
        WorldCipher dec = new WorldCipher(ZERO_KEY);

        byte[] wire = plain.clone();
        enc.encryptSend(wire);
        assertThat(wire).isNotEqualTo(plain);

        byte[] recv = padTo6(wire);
        dec.decryptRecv(recv);
        assertThat(new byte[]{recv[0], recv[1], recv[2], recv[3]}).isEqualTo(plain);
    }

    @Test
    void encryptSend_thenDecryptRecv_recoversPayload_seqKey() {
        byte[] plain = {0x0A, 0x00, (byte) 0xEC, 0x01};
        WorldCipher enc = new WorldCipher(seqKey());
        WorldCipher dec = new WorldCipher(seqKey());

        byte[] wire = plain.clone();
        enc.encryptSend(wire);

        byte[] recv = padTo6(wire);
        dec.decryptRecv(recv);

        assertThat(recv[0]).isEqualTo(plain[0]);
        assertThat(recv[1]).isEqualTo(plain[1]);
        assertThat(recv[2]).isEqualTo(plain[2]);
        assertThat(recv[3]).isEqualTo(plain[3]);
    }

    // ── send state continuity ─────────────────────────────────────────────────

    @Test
    void encryptSend_stateCarriesAcrossPackets() {
        WorldCipher stateful = new WorldCipher(seqKey());
        WorldCipher fresh    = new WorldCipher(seqKey());

        byte[] plain = {0x0A, 0x00, 0x01, 0x00};

        byte[] first = plain.clone();
        stateful.encryptSend(first);

        byte[] second = plain.clone();
        stateful.encryptSend(second);

        byte[] independent = plain.clone();
        fresh.encryptSend(independent);

        assertThat(first).isEqualTo(independent);     // packet 1 — same as fresh start
        assertThat(second).isNotEqualTo(independent); // packet 2 — state carried over
    }

    // ── key wrap ──────────────────────────────────────────────────────────────

    @Test
    void encryptSend_deterministicAcrossFullKeyCycle() {
        // Two ciphers with the same key should produce identical output for 11 packets
        // (10 × 4 = 40 bytes = one full key cycle, then wrap on packet 11)
        WorldCipher a = new WorldCipher(seqKey());
        WorldCipher b = new WorldCipher(seqKey());
        byte[] plain = {0x01, 0x02, 0x03, 0x04};

        for (int i = 0; i < 11; i++) {
            byte[] pa = plain.clone();
            byte[] pb = plain.clone();
            a.encryptSend(pa);
            b.encryptSend(pb);
            assertThat(pa).as("packet %d", i).isEqualTo(pb);
        }
    }

    // ── golden values ─────────────────────────────────────────────────────────

    @Test
    void encryptSend_goldenValue() {
        // key={1,2,...,40}, plain={0x12,0x34,0x56,0x78}, i=0, j=0
        // byte 0: (0x12 ^ 0x01) + 0x00 = 0x13; j=0x13
        // byte 1: (0x34 ^ 0x02) + 0x13 = 0x49; j=0x49
        // byte 2: (0x56 ^ 0x03) + 0x49 = 0x9E; j=0x9E
        // byte 3: (0x78 ^ 0x04) + 0x9E = 0x1A; j=0x1A  (byte overflow: 0x7C+0x9E=0x11A)
        byte[] data = {0x12, 0x34, 0x56, 0x78};
        new WorldCipher(seqKey()).encryptSend(data);

        assertThat(data[0] & 0xFF).isEqualTo(0x13);
        assertThat(data[1] & 0xFF).isEqualTo(0x49);
        assertThat(data[2] & 0xFF).isEqualTo(0x9E);
        assertThat(data[3] & 0xFF).isEqualTo(0x1A);
    }

    @Test
    void decryptRecv_goldenValue() {
        // key={1,2,...,40}, i=0, j=0
        // wire={0x0B,0x0D,0xFB,0x00,0x05,0x0B} decrypts to:
        //   size=10 (0x000A), opcode=0x000001ED = C_MSG_AUTH_SESSION
        // byte 0: (0x0B - 0x00) ^ 0x01 = 0x0A; j=0x0B
        // byte 1: (0x0D - 0x0B) ^ 0x02 = 0x00; j=0x0D
        // byte 2: (0xFB - 0x0D) ^ 0x03 = 0xED; j=0xFB
        // byte 3: (0x00 - 0xFB) ^ 0x04 = 0x01; j=0x00  (0x00-0xFB = -0xFB → 0x05 as byte, ^0x04=0x01)
        // byte 4: (0x05 - 0x00) ^ 0x05 = 0x00; j=0x05
        // byte 5: (0x0B - 0x05) ^ 0x06 = 0x00; j=0x0B
        byte[] wire = {0x0B, 0x0D, (byte) 0xFB, 0x00, 0x05, 0x0B};
        new WorldCipher(seqKey()).decryptRecv(wire);

        assertThat(wire[0] & 0xFF).isEqualTo(0x0A);
        assertThat(wire[1] & 0xFF).isEqualTo(0x00);
        assertThat(wire[2] & 0xFF).isEqualTo(0xED);
        assertThat(wire[3] & 0xFF).isEqualTo(0x01);
        assertThat(wire[4] & 0xFF).isEqualTo(0x00);
        assertThat(wire[5] & 0xFF).isEqualTo(0x00);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static byte[] seqKey() {
        byte[] key = new byte[40];
        for (int i = 0; i < 40; i++) key[i] = (byte) (i + 1);
        return key;
    }

    private static byte[] padTo6(byte[] src) {
        byte[] out = new byte[6];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, 6));
        return out;
    }
}
