package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Player character display (model) IDs from ChrRaces.dbc (vanilla 1.12.1).
 * Fields model_m / model_f map to UNIT_FIELD_DISPLAYID / UNIT_FIELD_NATIVEDISPLAYID.
 */
@Getter
@RequiredArgsConstructor
public enum DisplayId {

    HUMAN      (Race.HUMAN,     49,   50),
    ORC        (Race.ORC,       51,   52),
    DWARF      (Race.DWARF,     53,   54),
    NIGHT_ELF  (Race.NIGHT_ELF, 55,   56),
    UNDEAD     (Race.UNDEAD,    57,   58),
    TAUREN     (Race.TAUREN,    59,   60),
    GNOME      (Race.GNOME,     1563, 1564),
    TROLL      (Race.TROLL,     1478, 1479);

    private final Race race;
    private final int  male;
    private final int  female;

    public static int forCharacter(Race race, Gender gender) {
        for (DisplayId entry : values()) {
            if (entry.race == race) {
                return gender == Gender.FEMALE ? entry.female : entry.male;
            }
        }
        throw new IllegalArgumentException("No display ID for race=" + race + " gender=" + gender);
    }
}