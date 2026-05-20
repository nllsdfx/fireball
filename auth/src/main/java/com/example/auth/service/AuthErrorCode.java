package com.example.auth.service;

public enum AuthErrorCode {
    UNKNOWN_ACCOUNT(0x04),
    ACCOUNT_BANNED(0x06),
    ACCOUNT_SUSPENDED(0x07),
    NO_ACCESS(0x08);

    private final byte code;

    AuthErrorCode(int code) {
        this.code = (byte) code;
    }

    public byte code() {
        return code;
    }
}
