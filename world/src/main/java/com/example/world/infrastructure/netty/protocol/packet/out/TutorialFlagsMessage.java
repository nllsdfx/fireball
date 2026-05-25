package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

/**
 * SMSG_TUTORIAL_FLAGS — 8 × uint32 bitmasks marking completed tutorial steps.
 * We have no tutorial data, so all bits are zero.
 */
public record TutorialFlagsMessage() implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_TUTORIAL_FLAGS;
    }

    @Override
    public void encode(ByteBuf buf) {
        for (int i = 0; i < 8; i++) {
            buf.writeIntLE(0);
        }
    }
}