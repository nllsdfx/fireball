package com.example.auth.infrastructure.netty.protocol;

import com.example.auth.infrastructure.netty.protocol.packet.out.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@ChannelHandler.Sharable
@Component
public class AuthPacketEncoder extends MessageToByteEncoder<AuthResponse> {

    private static final int SRP6_KEY_LENGTH = 32;
    private static final int SHA1_DIGEST_LENGTH = 20;
    private static final int CRC_SALT_LENGTH = 16;
    private static final int SAFE_PRIME_LENGTH = 32;
    private static final int SALT_LENGTH = 32;

    @Override
    protected void encode(ChannelHandlerContext ctx, AuthResponse msg, ByteBuf out) {
        switch (msg) {
            case LogonChallengeResponse r -> encodeLogonChallenge(r, out);
            case LogonProofResponse r -> encodeLogonProof(r, out);
            case RealmListResponse r -> encodeRealmList(r, out);
        }
    }

    private void encodeLogonChallenge(LogonChallengeResponse r, ByteBuf out) {
        out.writeByte(AuthOpcode.CMD_AUTH_LOGON_CHALLENGE.getCode());
        out.writeByte(0x00); // unk
        out.writeByte(r.error());
        if (r.error() != 0) return;

        out.writeBytes(r.serverKey());             // B — 32 bytes
        out.writeByte(1);                    // g_len
        out.writeByte(r.generator());              // g
        out.writeByte(SAFE_PRIME_LENGTH);          // N_len
        out.writeBytes(r.safePrime());             // N — 32 bytes
        out.writeBytes(r.salt());                  // s — 32 bytes
        out.writeBytes(r.crcSalt());               // 16 bytes
        out.writeByte(r.securityFlags());
    }

    private void encodeLogonProof(LogonProofResponse r, ByteBuf out) {
        out.writeByte(AuthOpcode.CMD_AUTH_LOGON_PROOF.getCode());
        out.writeByte(r.error());
        if (r.error() != 0) return;

        out.writeBytes(r.serverProof());           // M2 — 20 bytes
        out.writeIntLE(r.accountFlags());          // LoginFlags (1.12: always 0; TBC+ adds surveyId + unkFlags)
    }

    private void encodeRealmList(RealmListResponse r, ByteBuf out) {
        out.writeByte(AuthOpcode.CMD_REALM_LIST.getCode());

        int sizeIndex = out.writerIndex();
        out.writeShortLE(0);                       // size placeholder

        int payloadStart = out.writerIndex();
        out.writeIntLE(0);                         // unk
        out.writeByte(r.realms().size());
        for (RealmEntry realm : r.realms()) {
            out.writeIntLE(realm.icon());          // uint32 in 1.12 wire format
            out.writeByte(realm.flags());
            writeNullTerminated(out, realm.name());
            writeNullTerminated(out, realm.address());
            out.writeFloatLE(realm.population());
            out.writeByte(realm.numChars());
            out.writeByte(realm.timezone());
            out.writeByte(realm.realmId());
        }
        out.writeShortLE(0x0002);                  // unk2 (1.12 trailer; TBC+ uses 0x0010)

        int payloadSize = out.writerIndex() - payloadStart;
        out.setShortLE(sizeIndex, payloadSize);
    }

    private void writeNullTerminated(ByteBuf out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
        out.writeByte(0x00);
    }
}
