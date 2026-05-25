package com.example.world.infrastructure.netty.protocol.updateobject;

import com.example.world.domain.model.Position;
import io.netty.buffer.ByteBuf;

/**
 * Movement block written inside SMSG_UPDATE_OBJECT for a living unit.
 *
 * For a standing player (self), the layout is:
 *   uint8  updateFlags  — bitmask: LIVING(0x20)|SELF(0x01)|ALL(0x10)|HAS_POSITION(0x40) = 0x71
 *   --- UPDATEFLAG_LIVING block ---
 *   uint32 moveFlags    — movement state flags (0 = standing still)
 *   uint32 stime        — server timestamp (0 for standing)
 *   float  x, y, z, o  — position
 *   uint32 fallTime     — ms since last fall start (0)
 *   float  speeds[6]    — walk, run, runBack, swim, swimBack, turnRate
 *   --- UPDATEFLAG_ALL block ---
 *   uint32 = 1
 */
public record MovementBlock(Position position) {

    // Update flags (Object.cpp BuildMovementUpdate, Unit.cpp constructor)
    // m_updateFlag = UPDATEFLAG_ALL | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION = 0x70
    // + UPDATEFLAG_SELF (added when building packet for the player themselves) = 0x71
    // Note: UPDATEFLAG_HAS_POSITION is present but its block is skipped by the client
    //       when UPDATEFLAG_LIVING is also set (position is already inside MovementInfo).
    private static final int UPDATE_FLAG_SELF         = 0x01;
    private static final int UPDATE_FLAG_ALL          = 0x10;
    private static final int UPDATE_FLAG_LIVING       = 0x20;
    private static final int UPDATE_FLAG_HAS_POSITION = 0x40;

    private static final int UPDATE_FLAGS =
            UPDATE_FLAG_SELF | UPDATE_FLAG_ALL | UPDATE_FLAG_LIVING | UPDATE_FLAG_HAS_POSITION;

    // Base movement speeds (Unit.cpp baseMoveSpeed[])
    private static final float SPEED_WALK      = 2.5f;
    private static final float SPEED_RUN       = 7.0f;
    private static final float SPEED_RUN_BACK  = 4.5f;
    private static final float SPEED_SWIM      = 4.722222f;
    private static final float SPEED_SWIM_BACK = 2.5f;
    private static final float SPEED_TURN_RATE = 3.141594f;

    public void encode(ByteBuf buf) {
        buf.writeByte(UPDATE_FLAGS);

        // MovementInfo (Unit.cpp MovementInfo::Write)
        buf.writeIntLE(0);                          // moveFlags = MOVEFLAG_NONE
        buf.writeIntLE(0);                          // stime
        buf.writeFloatLE(position.getX());
        buf.writeFloatLE(position.getY());
        buf.writeFloatLE(position.getZ());
        buf.writeFloatLE(position.getOrientation());
        buf.writeIntLE(0);                          // fallTime

        // Speeds
        buf.writeFloatLE(SPEED_WALK);
        buf.writeFloatLE(SPEED_RUN);
        buf.writeFloatLE(SPEED_RUN_BACK);
        buf.writeFloatLE(SPEED_SWIM);
        buf.writeFloatLE(SPEED_SWIM_BACK);
        buf.writeFloatLE(SPEED_TURN_RATE);

        // UPDATEFLAG_ALL
        buf.writeIntLE(1);
    }
}