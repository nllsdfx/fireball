package com.example.world.infrastructure.netty.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum WorldOpcode {

    S_MSG_AUTH_CHALLENGE(0x1EC),
    C_MSG_AUTH_SESSION(0x1ED),
    S_MSG_AUTH_RESPONSE(0x1EE),
    S_MSG_ADDON_INFO(0x2EF);

    private final int code;

    private static final Map<Integer, WorldOpcode> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(WorldOpcode::getCode, op -> op));

    public static WorldOpcode fromCode(int code) {
        return BY_CODE.get(code);
    }
}
