package com.example.world.infrastructure.netty.protocol.packet.out;

// SMSG_ADDON_INFO — one entry per addon found in CMSG_AUTH_SESSION's compressed list
public record AddonInfoMessage(int addonCount) implements ServerMessage {
}
