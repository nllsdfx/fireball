package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

/**
 * SMSG_INITIALIZE_FACTIONS — sends the player's faction standings (64 slots).
 * Each slot: uint8 flags + uint32 standing. We have no reputation data, so all zeros.
 */
public record InitializeFactionMessage() implements ServerMessage {

    private static final int FACTION_COUNT = 64;

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_INITIALIZE_FACTIONS;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(FACTION_COUNT);
        for (int i = 0; i < FACTION_COUNT; i++) {
            buf.writeByte(0);    // flags
            buf.writeIntLE(0);   // standing
        }
    }
}