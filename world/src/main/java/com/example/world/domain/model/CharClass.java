package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CharClass {
    WARRIOR(1),
    PALADIN(2),
    HUNTER(3),
    ROGUE(4),
    PRIEST(5),
    SHAMAN(7),
    MAGE(8),
    WARLOCK(9),
    DRUID(11);

    private final int id;
}