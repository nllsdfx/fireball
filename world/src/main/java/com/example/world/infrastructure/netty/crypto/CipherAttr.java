package com.example.world.infrastructure.netty.crypto;

import io.netty.util.AttributeKey;

public final class CipherAttr {

    public static final AttributeKey<WorldCipher> KEY = AttributeKey.valueOf("world_cipher");

    private CipherAttr() {}
}
