package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public sealed interface ServerMessage
        permits AddonInfoMessage, AuthChallengeMessage, AuthResponseMessage, CharEnumResponse, PongMessage {

    WorldOpcode opcode();

    void encode(ByteBuf buf);
}