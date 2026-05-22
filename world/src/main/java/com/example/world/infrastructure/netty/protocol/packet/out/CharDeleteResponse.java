package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

public record CharDeleteResponse(Result result) implements ServerMessage {

    public enum Result {
        SUCCESS(0x39),
        FAILED (0x3A);

        private final int code;
        Result(int code) { this.code = code; }
    }

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_CHAR_DELETE;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(result.code);
    }
}
