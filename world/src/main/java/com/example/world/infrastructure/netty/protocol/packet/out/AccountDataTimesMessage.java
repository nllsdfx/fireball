package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

/**
 * SMSG_ACCOUNT_DATA_TIMES — MD5 hash (16 bytes) for each of 8 account data slots
 * (keybindings, macros, etc.). We store nothing, so all hashes are zero.
 */
public record AccountDataTimesMessage() implements ServerMessage {

    private static final int SLOTS = 8;
    private static final int HASH_SIZE = 16;

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_ACCOUNT_DATA_TIMES;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeZero(SLOTS * HASH_SIZE);
    }
}