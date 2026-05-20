package com.example.auth.infrastructure.netty.protocol;

import com.example.auth.infrastructure.netty.protocol.packet.in.LogonChallengePacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.LogonProofPacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.RealmListRequestPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class AuthPacketDecoderTest {

    @Test
    void decodesLogonChallengePacket() {
        ByteBuf buf = buildChallengeBytes("ADMIN");
        EmbeddedChannel ch = new EmbeddedChannel(new AuthPacketDecoder());
        ch.writeInbound(buf);

        LogonChallengePacket packet = ch.readInbound();
        assertThat(packet).isNotNull();
        assertThat(packet.accountName()).isEqualTo("ADMIN");
        assertThat(packet.gameName()).isEqualTo("WoW");
        assertThat(packet.version()).isEqualTo(new byte[]{1, 12, 1});
        assertThat(packet.build()).isEqualTo((short) 5875);
        assertThat(packet.platform()).isEqualTo("x86");
        assertThat(packet.os()).isEqualTo("Win");
        assertThat(packet.locale()).isEqualTo("enUS");
        ch.finishAndReleaseAll();
    }

    @Test
    void decodesLogonProofPacket() {
        byte[] clientKey = new byte[32]; Arrays.fill(clientKey, (byte) 0x11);
        byte[] m1        = new byte[20]; Arrays.fill(m1,        (byte) 0x22);
        byte[] crcHash   = new byte[20]; Arrays.fill(crcHash,   (byte) 0x33);

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x01);        // opcode
        buf.writeBytes(clientKey);
        buf.writeBytes(m1);
        buf.writeBytes(crcHash);
        buf.writeByte(0);           // numberOfKeys
        buf.writeByte(0);           // securityFlags

        EmbeddedChannel ch = new EmbeddedChannel(new AuthPacketDecoder());
        ch.writeInbound(buf);

        LogonProofPacket packet = ch.readInbound();
        assertThat(packet).isNotNull();
        assertThat(packet.clientPublicKey()).isEqualTo(clientKey);
        assertThat(packet.clientProof()).isEqualTo(m1);
        assertThat(packet.crcHash()).isEqualTo(crcHash);
        ch.finishAndReleaseAll();
    }

    @Test
    void decodesRealmListRequest() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x10);         // opcode
        buf.writeIntLE(0x00000000);  // unknown

        EmbeddedChannel ch = new EmbeddedChannel(new AuthPacketDecoder());
        ch.writeInbound(buf);

        RealmListRequestPacket packet = ch.readInbound();
        assertThat(packet).isNotNull();
        assertThat(packet.unknown()).isZero();
        ch.finishAndReleaseAll();
    }

    @Test
    void unknownOpcode_throwsIllegalArgumentException() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0xFF); // unknown opcode

        EmbeddedChannel ch = new EmbeddedChannel(new AuthPacketDecoder());
        assertThatThrownBy(() -> ch.writeInbound(buf))
                .hasCauseInstanceOf(IllegalArgumentException.class);
        ch.finishAndReleaseAll();
    }

    // --- helpers ---

    private static ByteBuf buildChallengeBytes(String accountName) {
        byte[] nameBytes = accountName.getBytes(StandardCharsets.US_ASCII);
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x00);         // opcode CMD_AUTH_LOGON_CHALLENGE
        buf.writeByte(0x08);         // error (unk in 1.12, always 8)

        // size = everything after this field
        int sizeIndex = buf.writerIndex();
        buf.writeShortLE(0);

        int payloadStart = buf.writerIndex();
        buf.writeBytes("WoW\0".getBytes(StandardCharsets.US_ASCII)); // gameName
        buf.writeByte(1); buf.writeByte(12); buf.writeByte(1);       // version 1.12.1
        buf.writeShortLE(5875);                                       // build
        // platform, os, locale — sent reversed, decoder un-reverses
        writeReversed(buf, "x86\0");
        writeReversed(buf, "Win\0");
        writeReversed(buf, "enUS");
        buf.writeIntLE(0);           // timezoneBias
        buf.writeBytes(new byte[]{127, 0, 0, 1}); // ip
        buf.writeByte(nameBytes.length);
        buf.writeBytes(nameBytes);

        buf.setShortLE(sizeIndex, buf.writerIndex() - payloadStart);
        return buf;
    }

    private static void writeReversed(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        for (int i = bytes.length - 1; i >= 0; i--) buf.writeByte(bytes[i]);
    }
}
