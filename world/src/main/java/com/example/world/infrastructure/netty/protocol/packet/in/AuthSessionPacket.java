package com.example.world.infrastructure.netty.protocol.packet.in;

// CMSG_AUTH_SESSION — client proof sent after SMSG_AUTH_CHALLENGE
public record AuthSessionPacket(
        int clientBuild,
        String account,
        int clientSeed,
        byte[] digest,      // SHA1, 20 bytes
        int addonCount
) {
}
