package com.example.world.infrastructure.netty.protocol.updateobject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Indices of update fields in the flat field array sent in SMSG_UPDATE_OBJECT.
 * Each field occupies 4 bytes (one uint32 slot); GUID occupies two consecutive slots.
 *
 * Values are absolute indices computed from UpdateFields.h:
 *   OBJECT_END = 6, UNIT_END = 188, PLAYER_END = 1282.
 *
 * Only fields required for the player login update are listed here.
 */
@Getter
@RequiredArgsConstructor
public enum UpdateField {

    // Object (0–5)
    OBJECT_FIELD_GUID        (0),   // size 2 (uint64 = two uint32 slots)
    OBJECT_FIELD_TYPE        (2),
    OBJECT_FIELD_SCALE_X     (4),

    // Unit (6–187)
    UNIT_FIELD_HEALTH        (22),
    UNIT_FIELD_MAXHEALTH     (28),
    UNIT_FIELD_LEVEL         (34),
    UNIT_FIELD_FACTIONTEMPLATE(35),
    UNIT_FIELD_BYTES_0       (36),  // race | class | gender | power_type
    UNIT_FIELD_DISPLAYID     (131),
    UNIT_FIELD_NATIVEDISPLAYID(132),

    // Player (188–1281)
    PLAYER_BYTES             (193), // skin | face | hairStyle | hairColor
    PLAYER_BYTES_2           (194), // facialHair | 0 | 0 | 0

    // Sentinel — not a real field, used to size the update mask
    PLAYER_END               (1282);

    private final int index;
}