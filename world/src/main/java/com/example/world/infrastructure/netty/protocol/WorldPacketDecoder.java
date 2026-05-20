package com.example.world.infrastructure.netty.protocol;

import com.example.world.infrastructure.netty.protocol.packet.in.AuthSessionPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class WorldPacketDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int size = in.readUnsignedShort();
        int opcodeCode = in.readIntLE();
        int bodyLen = Math.max(0, size - 4);

        WorldOpcode opcode = WorldOpcode.fromCode(opcodeCode);
        if (opcode == null) {
            log.warn("Unknown opcode 0x{} (size={}), dropping", Integer.toHexString(opcodeCode), size);
            in.skipBytes(bodyLen);
            return;
        }

        ByteBuf body = in.readRetainedSlice(bodyLen);
        try {
            switch (opcode) {
                case C_MSG_AUTH_SESSION -> out.add(decodeAuthSession(body));
                default -> log.warn("Unhandled opcode {}, dropping", opcode);
            }
        } finally {
            body.release();
        }
    }

    private AuthSessionPacket decodeAuthSession(ByteBuf buf) {
        int clientBuild = buf.readIntLE();
        int unk2 = buf.readIntLE();
        String account = readNullTerminatedString(buf);
        int clientSeed = buf.readIntLE();
        byte[] digest = new byte[20];
        buf.readBytes(digest);
        byte[] addonData = new byte[buf.readableBytes()];
        buf.readBytes(addonData);
        return new AuthSessionPacket(clientBuild, unk2, account, clientSeed, digest, addonData);
    }

    private String readNullTerminatedString(ByteBuf buf) {
        int len = buf.bytesBefore((byte) 0);
        String s = buf.readCharSequence(len, StandardCharsets.US_ASCII).toString();
        buf.skipBytes(1);
        return s;
    }
}
