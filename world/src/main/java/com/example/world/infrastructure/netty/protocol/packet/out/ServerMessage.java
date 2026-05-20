package com.example.world.infrastructure.netty.protocol.packet.out;

public sealed interface ServerMessage
        permits AuthChallengeMessage, AuthResponseMessage {
}
