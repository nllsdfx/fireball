package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

/**
 * SMSG_INIT_WORLD_STATES — initialises zone-specific world states (PvP objectives,
 * instance flags, etc.). Required for the client to start rendering after login.
 *
 * For regular open-world zones there are no state entries, so count = 0.
 * Each state entry would be: uint32 stateId + uint32 stateValue (not needed here).
 */
public record InitWorldStatesMessage(int mapId, int zoneId) implements ServerMessage {

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_INIT_WORLD_STATES;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(mapId);
        buf.writeIntLE(zoneId);
        buf.writeShortLE(0); // count = 0, no state entries
    }
}