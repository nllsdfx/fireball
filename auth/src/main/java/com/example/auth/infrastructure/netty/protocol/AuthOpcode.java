package com.example.auth.infrastructure.netty.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum AuthOpcode {

    CMD_AUTH_LOGON_CHALLENGE(0x00),
    CMD_AUTH_LOGON_PROOF(0x01),
    CMD_REALM_LIST(0x10);

    private final int code;

    private static final Map<Integer, AuthOpcode> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(AuthOpcode::getCode, o -> o));

    public static AuthOpcode fromCode(int code) {
        AuthOpcode opcode = BY_CODE.get(code);
        if (opcode == null) {
            throw new IllegalArgumentException("Unknown auth opcode: 0x%02X".formatted(code));
        }
        return opcode;
    }
}
