package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public record CharCreateResponse(Result result) implements ServerMessage {

    public enum Result {
        SUCCESS       (0x2E),
        ERROR         (0x2F),
        NAME_IN_USE   (0x31),
        DISABLED      (0x3A),
        SERVER_LIMIT  (0x34),
        ACCOUNT_LIMIT (0x35);

        private final int code;
        Result(int code) { this.code = code; }
    }

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_CHAR_CREATE;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(result.code);
    }
}