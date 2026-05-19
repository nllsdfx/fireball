package com.example.auth.infrastructure.netty.protocol.packet.in;

/**
 * CMD_AUTH_REALM_LIST (0x10) — client requests the realm list after successful login.
 *
 * @param unknown always 0x00000000 in 1.12 clients
 */
public record RealmListRequestPacket(int unknown) {}
