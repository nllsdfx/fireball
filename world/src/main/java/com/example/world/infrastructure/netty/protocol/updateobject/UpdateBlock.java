package com.example.world.infrastructure.netty.protocol.updateobject;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Flat storage for update field values (one uint32 slot per field index).
 *
 * After the mask is written, encode() writes only the slots whose bits
 * are set in the mask, in ascending index order — exactly what the client expects.
 */
public class UpdateBlock {

    private final int[] slots = new int[UpdateField.PLAYER_END.getIndex()];

    public void setInt(UpdateField field, int value) {
        slots[field.getIndex()] = value;
    }

    public void setFloat(UpdateField field, float value) {
        slots[field.getIndex()] = Float.floatToRawIntBits(value);
    }

    /** Stores a uint64 across two consecutive slots (lo, hi). */
    public void setLong(UpdateField field, long value) {
        int index = field.getIndex();
        slots[index]     = (int) value;
        slots[index + 1] = (int) (value >>> 32);
    }

    public void encode(ByteBuf buf, UpdateMask mask) {
        for (int i = 0; i < slots.length; i++) {
            if (mask.isSet(i)) {
                buf.writeIntLE(slots[i]);
            }
        }
    }
}