package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum RaceStartPosition {
    HUMAN     (Race.HUMAN,      0,   12,  -8949.95f,  -132.493f,   83.5312f, 0f),
    ORC       (Race.ORC,        1,   14,   -618.518f, -4251.67f,   38.718f,  0f),
    DWARF     (Race.DWARF,      0,    1,  -6240.32f,   331.033f,  382.758f,  6.17716f),
    NIGHT_ELF (Race.NIGHT_ELF,  1,  141,  10311.3f,    832.463f, 1326.41f,  5.69632f),
    UNDEAD    (Race.UNDEAD,     0,   85,   1676.71f,   1678.31f,  121.67f,   2.70526f),
    TAUREN    (Race.TAUREN,     1,  215,  -2917.58f,   -257.98f,   52.9968f, 0f),
    GNOME     (Race.GNOME,      0,    1,  -6240.32f,   331.033f,  382.758f,  0f),
    TROLL     (Race.TROLL,      1,   14,   -618.518f, -4251.67f,   38.718f,  0f);

    private final Race race;
    private final int mapId;
    private final int zone;
    private final float x;
    private final float y;
    private final float z;
    private final float orientation;

    public Position toPosition() {
        return new Position(mapId, x, y, z, orientation);
    }

    private static final Map<Race, RaceStartPosition> BY_RACE = Arrays.stream(values())
            .collect(Collectors.toMap(rsp -> rsp.race, rsp -> rsp));

    public static RaceStartPosition forRace(Race race) {
        RaceStartPosition result = BY_RACE.get(race);
        if (result == null) throw new IllegalArgumentException("No start position for race: " + race);
        return result;
    }
}