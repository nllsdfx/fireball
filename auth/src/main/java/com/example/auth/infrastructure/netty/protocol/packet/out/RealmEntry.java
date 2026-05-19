package com.example.auth.infrastructure.netty.protocol.packet.out;

/**
 * A single realm entry inside {@link RealmListResponse}.
 *
 * @param icon       realm type icon (0 = Normal, 1 = PvP, 6 = RP, 8 = RPPvP)
 * @param flags      realm flags (0x01 = invalid, 0x02 = offline, 0x04 = show version)
 * @param name       realm display name, null-terminated when encoded
 * @param address    "host:port" string, null-terminated when encoded
 * @param population population float (0.0 = low, 1.0 = medium, 2.0 = high)
 * @param numChars   number of characters this account has on the realm
 * @param timezone   realm timezone category
 * @param realmId    realm identifier byte
 */
public record RealmEntry(
        byte icon,
        byte flags,
        String name,
        String address,
        float population,
        byte numChars,
        byte timezone,
        byte realmId
) {}
