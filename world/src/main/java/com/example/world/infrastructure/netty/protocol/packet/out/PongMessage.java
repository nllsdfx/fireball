package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public record PongMessage(int sequence) implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_PONG;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(sequence);
    }
}