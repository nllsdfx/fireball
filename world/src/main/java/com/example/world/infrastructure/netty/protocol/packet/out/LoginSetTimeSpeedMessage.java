package com.example.world.infrastructure.netty.protocol.packet.out;

import com.example.world.infrastructure.netty.protocol.WorldOpcode;
import io.netty.buffer.ByteBuf;

import java.time.LocalDateTime;

/**
 * SMSG_LOGIN_SETTIMESPEED — current server time packed into a uint32 + game speed.
 *
 * Bit layout (mangos secsToTimeBitFields):
 *   bits 31-24  year - 2000
 *   bits 23-20  month (0-based)
 *   bits 19-14  day - 1
 *   bits 13-11  day-of-week (0 = Sunday)
 *   bits 10-6   hour
 *   bits  5-0   minute
 */
public record LoginSetTimeSpeedMessage() implements ServerMessage {

    private static final float GAME_SPEED = 0.01666667f;

    @Override
    public WorldOpcode opcode() {
        return WorldOpcode.S_MSG_LOGIN_SETTIMESPEED;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeIntLE(packTime(LocalDateTime.now()));
        buf.writeFloatLE(GAME_SPEED);
    }

    static int packTime(LocalDateTime t) {
        int year    = t.getYear() - 2000;
        int month   = t.getMonthValue() - 1; // 0-based
        int day     = t.getDayOfMonth() - 1;
        int dow     = t.getDayOfWeek().getValue() % 7; // Mon=1..Sun=7 → Sun=0
        int hour    = t.getHour();
        int minute  = t.getMinute();
        return (year << 24) | (month << 20) | (day << 14) | (dow << 11) | (hour << 6) | minute;
    }
}