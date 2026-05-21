package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public record AddonInfoMessage(int addonCount) implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_ADDON_INFO;
    }

    @Override
    public void encode(ByteBuf buf) {
        // 8 bytes per addon — cmangos AddonHandler.cpp standard entry
        for (int i = 0; i < addonCount; i++) {
            buf.writeByte(2);
            buf.writeByte(1);
            buf.writeByte(0);
            buf.writeZero(4);
            buf.writeByte(0);
        }
    }
}