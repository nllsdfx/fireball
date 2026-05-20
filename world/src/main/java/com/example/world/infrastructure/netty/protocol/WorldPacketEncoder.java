package com.example.world.infrastructure.netty.protocol;

import com.example.world.infrastructure.netty.crypto.CipherAttr;
import com.example.world.infrastructure.netty.crypto.WorldCipher;
import com.example.world.infrastructure.netty.protocol.packet.out.AddonInfoMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthChallengeMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthResponseMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.ServerMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.springframework.stereotype.Component;

/**
 * Encodes outgoing server messages into the SMSG wire format: size[2 BE] + opcode[2 LE] + body.
 * When a WorldCipher is active on the channel, encrypts the 4-byte header before sending.
 * SMSG_AUTH_CHALLENGE is always unencrypted (cipher not yet initialized at that point).
 */
@Component
@ChannelHandler.Sharable
public class WorldPacketEncoder extends MessageToByteEncoder<ServerMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) {
        int headerStart = out.writerIndex();
        switch (msg) {
            case AuthChallengeMessage m -> encodeAuthChallenge(m, out);
            case AuthResponseMessage m  -> encodeAuthResponse(m, out);
            case AddonInfoMessage m     -> encodeAddonInfo(m, out);
        }
        WorldCipher cipher = ctx.channel().attr(CipherAttr.KEY).get();
        if (cipher != null) {
            byte[] header = new byte[WorldCipher.SEND_LEN];
            out.getBytes(headerStart, header);
            cipher.encryptSend(header);
            out.setBytes(headerStart, header);
        }
    }

    private void encodeAuthChallenge(AuthChallengeMessage msg, ByteBuf out) {
        out.writeShort(6);
        out.writeShortLE(WorldOpcode.S_MSG_AUTH_CHALLENGE.getCode());
        out.writeIntLE(msg.seed());
    }

    private void encodeAuthResponse(AuthResponseMessage msg, ByteBuf out) {
        boolean ok = msg.result() == AuthResponseMessage.AUTH_OK;
        out.writeShort(ok ? 12 : 3);
        out.writeShortLE(WorldOpcode.S_MSG_AUTH_RESPONSE.getCode());
        out.writeByte(msg.result());
        if (ok) out.writeZero(9); // BillingTimeRemaining[4] + BillingPlanFlags[1] + BillingTimeRested[4]
    }

    private void encodeAddonInfo(AddonInfoMessage msg, ByteBuf out) {
        // 8 bytes per addon — cmangos AddonHandler.cpp standard entry
        int payloadSize = msg.addonCount() * 8;
        out.writeShort(2 + payloadSize);
        out.writeShortLE(WorldOpcode.S_MSG_ADDON_INFO.getCode());
        for (int i = 0; i < msg.addonCount(); i++) {
            out.writeByte(2);
            out.writeByte(1);
            out.writeByte(0);
            out.writeZero(4);
            out.writeByte(0);
        }
    }
}
