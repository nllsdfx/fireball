package com.example.auth.infrastructure.netty.protocol.packet.out;

import java.util.List;

/**
 * CMD_AUTH_REALM_LIST (0x10) — server's response with available realms.
 *
 * @param realms list of available realms
 */
public record RealmListResponse(List<RealmEntry> realms) implements AuthResponse {}
