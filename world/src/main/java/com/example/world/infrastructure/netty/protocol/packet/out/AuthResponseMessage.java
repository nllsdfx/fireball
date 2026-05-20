package com.example.world.infrastructure.netty.protocol.packet.out;

// SMSG_AUTH_RESPONSE — result of CMSG_AUTH_SESSION verification
public record AuthResponseMessage(byte result) implements ServerMessage {

    public static final byte AUTH_OK = 0x0C;
    public static final byte AUTH_FAILED = 0x0D;
    public static final byte AUTH_UNKNOWN_ACCOUNT = 0x15;
    public static final byte AUTH_VERSION_MISMATCH = 0x14;

    public static AuthResponseMessage ok() {
        return new AuthResponseMessage(AUTH_OK);
    }

    public static AuthResponseMessage failed() {
        return new AuthResponseMessage(AUTH_FAILED);
    }
}
