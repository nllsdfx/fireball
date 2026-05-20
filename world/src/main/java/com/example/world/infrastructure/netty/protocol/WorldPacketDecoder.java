package com.example.world.infrastructure.netty.protocol;

import com.example.world.infrastructure.netty.crypto.CipherAttr;
import com.example.world.infrastructure.netty.crypto.WorldCipher;
import com.example.world.infrastructure.netty.protocol.packet.in.AuthSessionPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * CMSG wire format: size[2 BE] + opcode[4 LE] + body[size-4].
 * After auth, the 6-byte header is encrypted with WorldCipher (stored in CipherAttr).
 *
 * Uses a two-phase checkpoint to avoid re-decrypting the header on body replay:
 *   HEADER → read+decrypt 6 bytes, checkpoint → BODY
 *   BODY   → read body bytes, checkpoint → HEADER
 */
@Slf4j
public class WorldPacketDecoder extends ReplayingDecoder<WorldPacketDecoder.Step> {

    enum Step { HEADER, BODY }

    private int pendingOpcode;
    private int pendingBodyLen;

    public WorldPacketDecoder() {
        super(Step.HEADER);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        switch (state()) {
            case HEADER -> {
                byte[] header = new byte[6];
                in.readBytes(header); // Signal thrown here if < 6 bytes — no cipher state consumed

                WorldCipher cipher = ctx.channel().attr(CipherAttr.KEY).get();
                if (cipher != null) cipher.decryptRecv(header);

                int rawSize = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);
                if (rawSize < 4) {
                    log.warn("Malformed CMSG header: size={} < 4, closing", rawSize);
                    ctx.close();
                    return;
                }
                pendingBodyLen = rawSize - 4;
                pendingOpcode  = (header[2] & 0xFF)
                               | ((header[3] & 0xFF) << 8)
                               | ((header[4] & 0xFF) << 16)
                               | ((header[5] & 0xFF) << 24);
                checkpoint(Step.BODY);
            }
            case BODY -> {
                ByteBuf body = in.readRetainedSlice(pendingBodyLen);
                try {
                    WorldOpcode opcode = WorldOpcode.fromCode(pendingOpcode);
                    if (opcode == null) {
                        log.warn("Unknown opcode 0x{}, dropping", Integer.toHexString(pendingOpcode));
                    } else {
                        switch (opcode) {
                            case C_MSG_AUTH_SESSION -> out.add(decodeAuthSession(body));
                            default -> log.warn("Unhandled opcode {}, dropping", opcode);
                        }
                    }
                } finally {
                    body.release();
                }
                checkpoint(Step.HEADER);
            }
        }
    }

    private AuthSessionPacket decodeAuthSession(ByteBuf buf) {
        int clientBuild = buf.readIntLE();
        buf.skipBytes(4); // reserved field
        String account = readNullTerminatedString(buf);
        int clientSeed = buf.readIntLE();
        byte[] digest = new byte[20];
        buf.readBytes(digest);
        byte[] addonData = new byte[buf.readableBytes()];
        buf.readBytes(addonData);
        return new AuthSessionPacket(clientBuild, account, clientSeed, digest, parseAddonCount(addonData));
    }

    /**
     * Decompresses the zlib addon list from CMSG_AUTH_SESSION and counts the entries.
     * Format: uint32 LE uncompressed size + zlib-compressed addon entries.
     * Each entry: null-terminated name + crc[4] + unk[4] + flag[1].
     */
    private int parseAddonCount(byte[] addonData) {
        if (addonData.length < 4) return 0;
        int uncompressedSize = ByteBuffer.wrap(addonData, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (uncompressedSize <= 0 || uncompressedSize > 0xFFFFF) return 0;

        byte[] decompressed = new byte[uncompressedSize];
        Inflater inflater = new Inflater();
        int actual;
        try {
            inflater.setInput(addonData, 4, addonData.length - 4);
            actual = inflater.inflate(decompressed);
        } catch (DataFormatException e) {
            log.warn("Addon data decompression failed: {}", e.getMessage());
            return 0;
        } finally {
            inflater.end();
        }

        int count = 0;
        int pos = 0;
        while (pos < actual) {
            while (pos < actual && decompressed[pos] != 0) pos++;
            pos++; // skip null terminator
            if (pos + 9 > actual) break; // crc[4] + unk[4] + flag[1]
            pos += 9;
            count++;
        }
        return count;
    }

    private String readNullTerminatedString(ByteBuf buf) {
        int len = buf.bytesBefore((byte) 0);
        String s = buf.readCharSequence(len, StandardCharsets.US_ASCII).toString();
        buf.skipBytes(1);
        return s;
    }
}
