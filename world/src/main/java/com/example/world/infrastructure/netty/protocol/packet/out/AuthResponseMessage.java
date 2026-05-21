package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

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

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_AUTH_RESPONSE;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(result);
        if (result == AUTH_OK) {
            buf.writeZero(9); // BillingTimeRemaining[4] + BillingPlanFlags[1] + BillingTimeRested[4]
        }
    }
}