package com.example.world.infrastructure.netty.crypto;

/**
 * WoW 1.12 per-connection stream cipher (port of cmangos AuthCrypt.cpp).
 * Not standard RC4 — the XOR and ADD are swapped between enc/dec directions.
 *
 * Encrypt send (SMSG header, 4 bytes): x = (plain ^ key[i]) + j;  j = x
 * Decrypt recv (CMSG header, 6 bytes): x = (wire  - j)     ^ key[i]; j = wire
 *
 * Key = session key K (40 bytes, little-endian), derived during SRP6 auth.
 */
public class WorldCipher {

    private static final int KEY_LEN = 40;
    public static final int SEND_LEN = 4;
    public static final int RECV_LEN = 6;

    private final byte[] key;
    private int sendI, sendJ;
    private int recvI, recvJ;

    public WorldCipher(byte[] sessionKey) {
        this.key = sessionKey.clone();
    }

    public void encryptSend(byte[] data) {
        for (int t = 0; t < SEND_LEN; t++) {
            sendI %= KEY_LEN;
            data[t] = (byte) (((data[t] & 0xFF) ^ (key[sendI] & 0xFF)) + sendJ);
            sendI++;
            sendJ = data[t] & 0xFF;
        }
    }

    public void decryptRecv(byte[] data) {
        for (int t = 0; t < RECV_LEN; t++) {
            recvI %= KEY_LEN;
            int wire = data[t] & 0xFF;
            data[t] = (byte) ((wire - recvJ) ^ (key[recvI] & 0xFF));
            recvI++;
            recvJ = wire;
        }
    }
}
