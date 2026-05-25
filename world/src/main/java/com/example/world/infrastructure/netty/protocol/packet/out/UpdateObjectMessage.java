package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.domain.model.Character;
import com.example.world.domain.model.CharClass;
import com.example.world.domain.model.DisplayId;
import com.example.world.domain.model.Race;
import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import com.example.world.infrastructure.netty.protocol.updateobject.MovementBlock;
import com.example.world.infrastructure.netty.protocol.updateobject.UpdateBlock;
import com.example.world.infrastructure.netty.protocol.updateobject.UpdateField;
import com.example.world.infrastructure.netty.protocol.updateobject.UpdateMask;
import io.netty.buffer.ByteBuf;

public record UpdateObjectMessage(Character character) implements ServerMessage {

    // UPDATETYPE_CREATE_OBJECT2 — used for self (Object.cpp)
    private static final int UPDATE_TYPE = 3;

    // TYPEID_PLAYER (ObjectGuid.h)
    private static final int OBJECT_TYPE_PLAYER = 4;

    // TYPEMASK_OBJECT | TYPEMASK_UNIT | TYPEMASK_PLAYER (ObjectGuid.h)
    private static final int TYPE_MASK = 0x01 | 0x08 | 0x10;

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_UPDATE_OBJECT;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(1);               // count: one object in this packet
        buf.writeByte(0);                // hasTransport: 0 = no transport objects
        buf.writeByte(UPDATE_TYPE);
        writePackedGuid(buf, character.getId());
        buf.writeByte(OBJECT_TYPE_PLAYER);

        new MovementBlock(character.getPosition()).encode(buf);

        UpdateMask mask  = new UpdateMask();
        UpdateBlock block = new UpdateBlock();
        fillFields(mask, block);

        mask.encode(buf);
        block.encode(buf, mask);
    }

    private void fillFields(UpdateMask mask, UpdateBlock block) {
        // GUID occupies two consecutive slots (lo + hi)
        mask.set(UpdateField.OBJECT_FIELD_GUID);
        mask.set(UpdateField.OBJECT_FIELD_GUID, 1); // hi slot
        block.setLong(UpdateField.OBJECT_FIELD_GUID, character.getId());

        mask.set(UpdateField.OBJECT_FIELD_TYPE);
        block.setInt(UpdateField.OBJECT_FIELD_TYPE, TYPE_MASK);

        mask.set(UpdateField.OBJECT_FIELD_SCALE_X);
        block.setFloat(UpdateField.OBJECT_FIELD_SCALE_X, 1.0f);

        mask.set(UpdateField.UNIT_FIELD_HEALTH);
        block.setInt(UpdateField.UNIT_FIELD_HEALTH, 100);

        mask.set(UpdateField.UNIT_FIELD_MAXHEALTH);
        block.setInt(UpdateField.UNIT_FIELD_MAXHEALTH, 100);

        mask.set(UpdateField.UNIT_FIELD_LEVEL);
        block.setInt(UpdateField.UNIT_FIELD_LEVEL, character.getLevel());

        mask.set(UpdateField.UNIT_FIELD_FACTIONTEMPLATE);
        block.setInt(UpdateField.UNIT_FIELD_FACTIONTEMPLATE, factionTemplate(character.getRace()));

        // BYTES_0: race | (class << 8) | (gender << 16) | (powerType << 24)
        mask.set(UpdateField.UNIT_FIELD_BYTES_0);
        int bytes0 = (character.getRace().getId() & 0xFF)
                   | ((character.getCharClass().getId() & 0xFF) << 8)
                   | ((character.getGender().getId() & 0xFF) << 16)
                   | (powerType(character.getCharClass()) << 24);
        block.setInt(UpdateField.UNIT_FIELD_BYTES_0, bytes0);

        int displayId = DisplayId.forCharacter(character.getRace(), character.getGender());
        mask.set(UpdateField.UNIT_FIELD_DISPLAYID);
        block.setInt(UpdateField.UNIT_FIELD_DISPLAYID, displayId);

        mask.set(UpdateField.UNIT_FIELD_NATIVEDISPLAYID);
        block.setInt(UpdateField.UNIT_FIELD_NATIVEDISPLAYID, displayId);

        // PLAYER_BYTES: skin | (face << 8) | (hairStyle << 16) | (hairColor << 24)
        mask.set(UpdateField.PLAYER_BYTES);
        int playerBytes = (character.getSkin() & 0xFF)
                        | ((character.getFace() & 0xFF) << 8)
                        | ((character.getHairStyle() & 0xFF) << 16)
                        | ((character.getHairColor() & 0xFF) << 24);
        block.setInt(UpdateField.PLAYER_BYTES, playerBytes);

        // PLAYER_BYTES_2: facialHair | 0 | 0 | 0
        mask.set(UpdateField.PLAYER_BYTES_2);
        block.setInt(UpdateField.PLAYER_BYTES_2, character.getFacialHair() & 0xFF);
    }

    /** Packs a uint64 GUID into the variable-length format (ByteBuffer.h appendPackGUID). */
    private static void writePackedGuid(ByteBuf buf, long guid) {
        int maskIndex = buf.writerIndex();
        buf.writeByte(0); // placeholder for bitmask
        byte mask = 0;
        for (int i = 0; guid != 0; i++, guid >>>= 8) {
            if ((guid & 0xFF) != 0) {
                mask |= (byte) (1 << i);
                buf.writeByte((int) (guid & 0xFF));
            }
        }
        buf.setByte(maskIndex, mask);
    }

    /** Power type for UNIT_FIELD_BYTES_0 byte 3 (Unit.h Powers enum). */
    private static int powerType(CharClass cls) {
        return switch (cls) {
            case WARRIOR -> 1; // POWER_RAGE
            case ROGUE   -> 3; // POWER_ENERGY
            default      -> 0; // POWER_MANA
        };
    }

    /** Faction template from ChrRaces.dbc FactionID field (vanilla 1.12.1). */
    private static int factionTemplate(Race race) {
        return switch (race) {
            case HUMAN     -> 1;
            case ORC       -> 2;
            case DWARF     -> 3;
            case NIGHT_ELF -> 4;
            case UNDEAD    -> 5;
            case TAUREN    -> 6;
            case GNOME     -> 115;
            case TROLL     -> 116;
        };
    }
}