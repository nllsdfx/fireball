package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.domain.model.Character;
import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.List;

public record CharEnumResponse(List<Character> characters) implements ServerMessage {

    private static final int EQUIPMENT_SLOTS = 20; // EQUIPMENT_SLOT_END(19) + bag slot 0

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_CHAR_ENUM;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(characters.size());
        for (Character c : characters) {
            buf.writeLongLE(c.getId());
            buf.writeBytes(c.getName().getBytes(StandardCharsets.US_ASCII));
            buf.writeByte(0); // null terminator
            buf.writeByte(c.getRace().getId());
            buf.writeByte(c.getCharClass().getId());
            buf.writeByte(c.getGender().getId());
            buf.writeByte(c.getSkin());
            buf.writeByte(c.getFace());
            buf.writeByte(c.getHairStyle());
            buf.writeByte(c.getHairColor());
            buf.writeByte(c.getFacialHair());
            buf.writeByte(c.getLevel());
            buf.writeIntLE(c.getZone());
            buf.writeIntLE(c.getPosition().getMapId());
            buf.writeFloatLE(c.getPosition().getX());
            buf.writeFloatLE(c.getPosition().getY());
            buf.writeFloatLE(c.getPosition().getZ());
            buf.writeIntLE(0); // guildId
            buf.writeIntLE(0); // charFlags
            buf.writeByte(0);  // firstLogin
            buf.writeIntLE(0); // petDisplayId
            buf.writeIntLE(0); // petLevel
            buf.writeIntLE(0); // petFamily
            for (int i = 0; i < EQUIPMENT_SLOTS; i++) {
                buf.writeIntLE(0); // displayInfoId
                buf.writeByte(0);  // inventoryType
            }
        }
    }
}