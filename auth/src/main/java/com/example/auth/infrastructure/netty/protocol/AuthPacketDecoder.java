package com.example.auth.infrastructure.netty.protocol;

import com.example.auth.infrastructure.netty.protocol.packet.in.LogonChallengePacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.LogonProofPacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.RealmListRequestPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuthPacketDecoder extends ReplayingDecoder<Void> {

    private static final int SRP6_KEY_LENGTH = 32;
    private static final int SHA1_DIGEST_LENGTH = 20;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        AuthOpcode opcode = AuthOpcode.fromCode(in.readUnsignedByte());
        out.add(switch (opcode) {
            case CMD_AUTH_LOGON_CHALLENGE -> decodeLogonChallenge(in);
            case CMD_AUTH_LOGON_PROOF -> decodeLogonProof(in);
            case CMD_REALM_LIST -> decodeRealmList(in);
        });
    }

    private LogonChallengePacket decodeLogonChallenge(ByteBuf in) {
        byte error = in.readByte();
        in.skipBytes(2); // packet size field — not needed with ReplayingDecoder

        String gameName = readString(in, 4);
        byte[] version = {in.readByte(), in.readByte(), in.readByte()};
        short build = in.readShortLE();
        String platform = readReversedString(in, 4);
        String os = readReversedString(in, 4);
        String locale = readReversedString(in, 4);
        int timezoneBias = in.readIntLE();

        byte[] ip = new byte[4];
        in.readBytes(ip);

        byte[] accountName = new byte[in.readUnsignedByte()];
        in.readBytes(accountName);

        return new LogonChallengePacket(
                error, gameName, version, build, platform, os, locale,
                timezoneBias, ip, new String(accountName, StandardCharsets.US_ASCII)
        );
    }

    private LogonProofPacket decodeLogonProof(ByteBuf in) {
        byte[] clientPublicKey = new byte[SRP6_KEY_LENGTH];
        in.readBytes(clientPublicKey);

        byte[] clientProof = new byte[SHA1_DIGEST_LENGTH];
        in.readBytes(clientProof);

        byte[] crcHash = new byte[SHA1_DIGEST_LENGTH];
        in.readBytes(crcHash);

        return new LogonProofPacket(clientPublicKey, clientProof, crcHash, in.readByte(), in.readByte());
    }

    private RealmListRequestPacket decodeRealmList(ByteBuf in) {
        return new RealmListRequestPacket(in.readIntLE());
    }

    private String readString(ByteBuf in, int length) {
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes, StandardCharsets.US_ASCII).replace("\0", "");
    }

    // platform/os/locale are sent as 4-byte little-endian reversed ASCII (e.g. "x86\0" → "0\68x")
    private String readReversedString(ByteBuf in, int length) {
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new StringBuilder(new String(bytes, StandardCharsets.US_ASCII))
                .reverse()
                .toString()
                .replace("\0", "")
                .trim();
    }
}
