package com.example.world.infrastructure.netty.protocol.packet.out;

// SMSG_AUTH_CHALLENGE — sent immediately on connect, unencrypted
public record AuthChallengeMessage(int seed) implements ServerMessage {
}
