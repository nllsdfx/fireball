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
    short skin;
    short face;
    short hairStyle;
    short hairColor;
    short facialHair;
    short level;
    Integer zone;
    Position position;
}