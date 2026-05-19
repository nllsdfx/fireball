package com.example.auth.infrastructure.netty.protocol.packet.in;

/**
 * CMD_AUTH_LOGON_CHALLENGE (0x00) — first packet sent by the client on connect.
 *
 * @param error        always 8 in 1.12 clients
 * @param gameName     "WoW\0"
 * @param version      client version, e.g. {1, 12, 1}
 * @param build        client build, e.g. 5875
 * @param platform     e.g. "x86"
 * @param os           e.g. "Win"
 * @param locale       e.g. "enUS"
 * @param timezoneBias UTC offset in minutes
 * @param ip           client IP as raw 4 bytes
 * @param accountName  account name (ASCII, uppercased by client)
 */
public record LogonChallengePacket(
        byte error,
        String gameName,
        byte[] version,
        short build,
        String platform,
        String os,
        String locale,
        int timezoneBias,
        byte[] ip,
        String accountName
) {}
