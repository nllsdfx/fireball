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
        for (int i = 0; i < addonCount; i++) {
            buf.writeByte(2);    // ADDON_STATUS_OK
            buf.writeByte(0);    // key_version=0: no public key (avoids client reading 256 garbage bytes)
            buf.writeZero(4);    // unk uint32
            buf.writeByte(0);    // no URL update
        }
    }
}