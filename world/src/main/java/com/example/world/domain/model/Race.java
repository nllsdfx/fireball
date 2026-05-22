package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Race {
    HUMAN((short) 1),
    ORC((short) 2),
    DWARF((short) 3),
    NIGHT_ELF((short) 4),
    UNDEAD((short) 5),
    TAUREN((short) 6),
    GNOME((short) 7),
    TROLL((short) 8);

    private final short id;

    private static final Map<Short, Race> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(Race::getId, r -> r));

    public static Race fromId(short id) { return BY_ID.get(id); }
}