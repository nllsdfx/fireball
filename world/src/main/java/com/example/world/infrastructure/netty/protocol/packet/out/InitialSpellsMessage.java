package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

/**
 * SMSG_INITIAL_SPELLS — sends the player's known spell list.
 * Format: uint8 unk=0, uint16 spellCount, [spells...], uint16 cooldownCount.
 * We have no spells yet, so both counts are zero.
 */
public record InitialSpellsMessage() implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_INITIAL_SPELLS;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(0);     // unk
        buf.writeShortLE(0);  // spell count
        buf.writeShortLE(0);  // cooldown count
    }
}