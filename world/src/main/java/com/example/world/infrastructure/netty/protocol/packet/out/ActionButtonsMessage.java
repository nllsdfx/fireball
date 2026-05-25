package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

/**
 * SMSG_ACTION_BUTTONS — sends all 120 action bar buttons (vanilla 1.12.1).
 * Each button is a uint32 packed value. We have no saved buttons, so all zeros.
 */
public record ActionButtonsMessage() implements ServerMessage {

    private static final int BUTTON_COUNT = 120;

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_ACTION_BUTTONS;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeZero(BUTTON_COUNT * 4);
    }
}