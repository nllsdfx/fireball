package com.example.auth.infrastructure.netty.protocol;

import com.example.auth.infrastructure.netty.protocol.packet.out.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AuthPacketEncoderTest {

    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new AuthPacketEncoder());
    }

    @AfterEach
    void tearDown() {
        channel.finishAndReleaseAll();
    }

    // --- LogonChallengeResponse ---

    @Test
    void encodeLogonChallengeError_is3Bytes() {
        channel.writeOutbound(LogonChallengeResponse.error((byte) 0x04));
        ByteBuf buf = channel.readOutbound();

        assertThat(buf.readableBytes()).isEqualTo(3);
        assertThat(buf.readByte()).isEqualTo((byte) 0x00);  // opcode
        assertThat(buf.readByte()).isEqualTo((byte) 0x00);  // unk
        assertThat(buf.readByte()).isEqualTo((byte) 0x04);  // error code
        buf.release();
    }

    @Test
    void encodeLogonChallengeSuccess_is119BytesWithCorrectStructure() {
        byte[] serverKey = fill(32, (byte) 0x11);
        byte[] safePrime = fill(32, (byte) 0x22);
        byte[] salt      = fill(32, (byte) 0x33);
        byte[] crcSalt   = fill(16, (byte) 0x44);
        var response = new LogonChallengeResponse(
                (byte) 0, serverKey, (byte) 7, safePrime, salt, crcSalt, (byte) 0);

        channel.writeOutbound(response);
        ByteBuf buf = channel.readOutbound();

        assertThat(buf.readableBytes()).isEqualTo(119);
        assertThat(buf.readByte()).isEqualTo((byte) 0x00); // opcode
        assertThat(buf.readByte()).isEqualTo((byte) 0x00); // unk
        assertThat(buf.readByte()).isEqualTo((byte) 0x00); // error = success
        assertBytesRead(buf, serverKey);                   // B
        assertThat(buf.readByte()).isEqualTo((byte) 1);    // g_len
        assertThat(buf.readByte()).isEqualTo((byte) 7);    // g
        assertThat(buf.readByte()).isEqualTo((byte) 32);   // N_len
        assertBytesRead(buf, safePrime);                   // N
        assertBytesRead(buf, salt);                        // s
        assertBytesRead(buf, crcSalt);                     // crc_salt
        assertThat(buf.readByte()).isEqualTo((byte) 0);    // securityFlags
        assertThat(buf.readableBytes()).isZero();
        buf.release();
    }

    // --- LogonProofResponse ---

    @Test
    void encodeLogonProofError_is2Bytes() {
        channel.writeOutbound(LogonProofResponse.error((byte) 0x04));
        ByteBuf buf = channel.readOutbound();

        assertThat(buf.readableBytes()).isEqualTo(2);
        assertThat(buf.readByte()).isEqualTo((byte) 0x01); // opcode
        assertThat(buf.readByte()).isEqualTo((byte) 0x04); // error code
        buf.release();
    }

    @Test
    void encodeLogonProofSuccess_is26Bytes() {
        // Regression: 1.12 expects exactly 26 bytes (cmd + error + M2[20] + LoginFlags[4]).
        // Sending TBC-style 32 bytes (+ surveyId + unkFlags) breaks realm list display.
        byte[] m2 = fill(20, (byte) 0xAA);
        channel.writeOutbound(new LogonProofResponse((byte) 0, m2, 0));
        ByteBuf buf = channel.readOutbound();

        assertThat(buf.readableBytes()).isEqualTo(26);
        assertThat(buf.readByte()).isEqualTo((byte) 0x01); // opcode
        assertThat(buf.readByte()).isEqualTo((byte) 0x00); // error = success
        assertBytesRead(buf, m2);                          // M2
        assertThat(buf.readIntLE()).isZero();               // LoginFlags
        assertThat(buf.readableBytes()).isZero();
        buf.release();
    }

    // --- RealmListResponse ---

    @Test
    void encodeRealmListEmpty_hasCorrectHeader() {
        channel.writeOutbound(new RealmListResponse(List.of()));
        ByteBuf buf = channel.readOutbound();

        assertThat(buf.readByte()).isEqualTo((byte) 0x10);  // opcode
        int payloadSize = buf.readShortLE();
        assertThat(payloadSize).isEqualTo(7);               // 4 (unk) + 1 (count) + 2 (trailer)
        assertThat(buf.readIntLE()).isZero();                // unk
        assertThat(buf.readByte()).isZero();                 // count = 0
        assertThat(buf.readShortLE()).isEqualTo((short) 0x0002); // trailer
        assertThat(buf.readableBytes()).isZero();
        buf.release();
    }

    @Test
    void encodeRealmListEntry_correctFieldOrder() {
        var entry = new RealmEntry((byte) 0, (byte) 0, "Test", "127.0.0.1:8085", 0.0f, (byte) 0, (byte) 1, (byte) 0);
        channel.writeOutbound(new RealmListResponse(List.of(entry)));
        ByteBuf buf = channel.readOutbound();

        assertThat(buf.readByte()).isEqualTo((byte) 0x10);  // opcode
        buf.readShortLE();                                   // size (skip)
        buf.readIntLE();                                     // unk
        assertThat(buf.readByte()).isEqualTo((byte) 1);     // count

        assertThat(buf.readIntLE()).isZero();                // icon uint32
        assertThat(buf.readByte()).isZero();                 // flags
        assertNullTerminatedString(buf, "Test");
        assertNullTerminatedString(buf, "127.0.0.1:8085");
        assertThat(buf.readFloatLE()).isZero();              // population
        assertThat(buf.readByte()).isZero();                 // numChars
        assertThat(buf.readByte()).isEqualTo((byte) 1);     // timezone
        assertThat(buf.readByte()).isZero();                 // realmId
        assertThat(buf.readShortLE()).isEqualTo((short) 0x0002); // trailer
        assertThat(buf.readableBytes()).isZero();
        buf.release();
    }

    // --- helpers ---

    private static byte[] fill(int size, byte value) {
        byte[] b = new byte[size]; Arrays.fill(b, value); return b;
    }

    private static void assertBytesRead(ByteBuf buf, byte[] expected) {
        byte[] actual = new byte[expected.length];
        buf.readBytes(actual);
        assertThat(actual).isEqualTo(expected);
    }

    private static void assertNullTerminatedString(ByteBuf buf, String expected) {
        byte[] chars = expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] actual = new byte[chars.length];
        buf.readBytes(actual);
        assertThat(actual).isEqualTo(chars);
        assertThat(buf.readByte()).isZero(); // null terminator
    }
}
