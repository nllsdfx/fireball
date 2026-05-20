package com.example.world.infrastructure.netty.protocol;

import com.example.world.infrastructure.netty.protocol.packet.out.AuthChallengeMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthResponseMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.ServerMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class WorldPacketEncoder extends MessageToByteEncoder<ServerMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) {
        switch (msg) {
            case AuthChallengeMessage m -> encodeAuthChallenge(m, out);
            case AuthResponseMessage m -> encodeAuthResponse(m, out);
        }
    }

    private void encodeAuthChallenge(AuthChallengeMessage msg, ByteBuf out) {
        out.writeShort(6);
        out.writeShortLE(WorldOpcode.S_MSG_AUTH_CHALLENGE.getCode());
        out.writeIntLE(msg.seed());
    }

    private void encodeAuthResponse(AuthResponseMessage msg, ByteBuf out) {
        if (msg.result() == AuthResponseMessage.AUTH_OK) {
            out.writeShort(12);
            out.writeShortLE(WorldOpcode.S_MSG_AUTH_RESPONSE.getCode());
            out.writeByte(msg.result());
            out.writeZero(9); // BillingTimeRemaining[4] + BillingPlanFlags[1] + BillingTimeRested[4]
        } else {
            out.writeShort(3);
            out.writeShortLE(WorldOpcode.S_MSG_AUTH_RESPONSE.getCode());
            out.writeByte(msg.result());
        }
    }
}
