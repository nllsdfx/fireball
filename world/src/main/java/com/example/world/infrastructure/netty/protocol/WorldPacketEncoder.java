package com.example.world.infrastructure.netty.protocol;

import com.example.world.infrastructure.netty.crypto.CipherAttr;
import com.example.world.infrastructure.netty.crypto.WorldCipher;
import com.example.world.infrastructure.netty.protocol.packet.out.ServerMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.springframework.stereotype.Component;

/**
 * SMSG wire format: size[2 BE] + opcode[2 LE] + body.
 * size = 2 (opcode) + body length.
 * Header is encrypted with WorldCipher once session is authenticated.
 */
@Component
@ChannelHandler.Sharable
public class WorldPacketEncoder extends MessageToByteEncoder<ServerMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) {
        int sizeIndex = out.writerIndex();
        out.writeShort(0); // placeholder — backpatched after body is written
        out.writeShortLE(msg.opcode().getCode());
        int bodyStart = out.writerIndex();
        msg.encode(out);
        out.setShort(sizeIndex, 2 + (out.writerIndex() - bodyStart));

        WorldCipher cipher = ctx.channel().attr(CipherAttr.KEY).get();
        if (cipher != null) {
            byte[] header = new byte[WorldCipher.SEND_LEN];
            out.getBytes(sizeIndex, header);
            cipher.encryptSend(header);
            out.setBytes(sizeIndex, header);
        }
    }
}