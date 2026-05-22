package com.example.world.infrastructure.netty.protocol.packet.in;

public record CharCreateRequest(
        String name,
        short race,
        short charClass,
        short gender,
        short skin,
        short face,
        short hairStyle,
        short hairColor,
        short facialHair
) {}