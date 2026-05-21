package com.example.world.domain.model;

import lombok.Value;

@Value
public class Character {
    Long id;
    Long accountId;
    String name;
    Race race;
    CharClass charClass;
    Gender gender;
    int skin;
    int face;
    int hairStyle;
    int hairColor;
    int facialHair;
    int level;
    Integer zone;
    Position position;
}