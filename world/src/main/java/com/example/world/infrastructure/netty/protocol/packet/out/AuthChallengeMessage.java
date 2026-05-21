package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public record AuthChallengeMessage(int seed) implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_AUTH_CHALLENGE;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(seed);
    }
}