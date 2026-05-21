package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public sealed interface ServerMessage
        permits AuthChallengeMessage, AuthResponseMessage, AddonInfoMessage, CharEnumResponse {

    WorldOpcode opcode();

    void encode(ByteBuf buf);
}