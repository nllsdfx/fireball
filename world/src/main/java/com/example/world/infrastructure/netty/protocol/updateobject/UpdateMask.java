package com.example.world.infrastructure.netty.protocol.updateobject;

import io.netty.buffer.ByteBuf;

/**
 * Bitmask that tracks which update fields are present in a SMSG_UPDATE_OBJECT packet.
 *
 * Each field index maps to one bit: word = index / 32, bit = index % 32.
 * The mask is written as WORD_COUNT uint32-LE words before the field values.
 */
public class UpdateMask {

    private static final int WORD_COUNT = (UpdateField.PLAYER_END.getIndex() + 31) / 32; // 41

    private final int[] words = new int[WORD_COUNT];

    public void set(UpdateField field) {
        set(field.getIndex());
    }

    /** Sets the bit at field.index + offset — for multi-slot fields like GUID (size 2). */
    public void set(UpdateField field, int offset) {
        set(field.getIndex() + offset);
    }

    private void set(int index) {
        words[index / 32] |= (1 << (index % 32));
    }

    boolean isSet(int index) {
        return index < words.length * 32 && (words[index / 32] & (1 << (index % 32))) != 0;
    }

    public void encode(ByteBuf buf) {
        buf.writeByte(WORD_COUNT);
        for (int word : words) {
            buf.writeIntLE(word);
        }
    }
}