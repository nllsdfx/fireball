package com.example.world.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE((short) 0),
    FEMALE((short) 1);

    private final short id;

    private static final Map<Short, Gender> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(Gender::getId, g -> g));

    public static Gender fromId(short id) { return BY_ID.get(id); }
}