package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum CharClass {
    WARRIOR((short) 1),
    PALADIN((short) 2),
    HUNTER((short) 3),
    ROGUE((short) 4),
    PRIEST((short) 5),
    SHAMAN((short) 7),
    MAGE((short) 8),
    WARLOCK((short) 9),
    DRUID((short) 11);

    private final short id;

    private static final Map<Short, CharClass> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CharClass::getId, c -> c));

    public static CharClass fromId(short id) { return BY_ID.get(id); }
}