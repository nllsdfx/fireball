package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public record LoginVerifyWorldMessage(int mapId, float x, float y, float z, float orientation)
        implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_LOGIN_VERIFY_WORLD;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(mapId);
        buf.writeFloatLE(x);
        buf.writeFloatLE(y);
        buf.writeFloatLE(z);
        buf.writeFloatLE(orientation);
    }
}