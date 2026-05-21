package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Race {
    HUMAN(1),
    ORC(2),
    DWARF(3),
    NIGHT_ELF(4),
    UNDEAD(5),
    TAUREN(6),
    GNOME(7),
    TROLL(8);

    private final int id;
}