package com.example.world.infrastructure.netty.session;

import com.example.world.infrastructure.netty.protocol.WorldPacketDecoder;
import com.example.world.infrastructure.netty.protocol.WorldPacketEncoder;
import com.example.world.infrastructure.netty.protocol.packet.in.PingRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.PongMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PingPongTest {

    // ── decoder ──────────────────────────────────────────────────────────────

    @Test
    void decoder_cmsgPing_yieldsPingRequest() {
        EmbeddedChannel ch = new EmbeddedChannel(new WorldPacketDecoder());
        ch.writeInbound(buildPingFrame(42));

        PingRequest pkt = ch.readInbound();
        assertThat(pkt).isNotNull();
        assertThat(pkt.sequence()).isEqualTo(42);
        ch.finishAndReleaseAll();
    }

    // ── handler ───────────────────────────────────────────────────────────────

    @Test
    void worldHandler_pingRequest_writesMatchingPong() {
        WorldHandler handler = new WorldHandler();
        handler.register();

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        WorldSession session = mock(WorldSession.class);
        when(session.getState()).thenReturn(WorldSession.State.IN_WORLD);
        when(session.getCtx()).thenReturn(ctx);

        handler.dispatch(session, new PingRequest(99));

        verify(ctx).writeAndFlush(new PongMessage(99));
    }

    // ── encoder ───────────────────────────────────────────────────────────────

    @Test
    void encoder_pongMessage_writesSequenceLE() {
        EmbeddedChannel ch = new EmbeddedChannel(new WorldPacketEncoder());
        ch.writeOutbound(new PongMessage(0x12345678));

        ByteBuf out = ch.readOutbound();
        assertThat(out.readUnsignedShort()).isEqualTo(6);          // size BE: 2 (opcode) + 4 (body)
        assertThat(out.readUnsignedShortLE()).isEqualTo(0x1DD);    // S_MSG_PONG opcode LE
        assertThat(out.readIntLE()).isEqualTo(0x12345678);         // sequence LE
        out.release();
        ch.finishAndReleaseAll();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    // CMSG wire: size[2 BE] + opcode[4 LE] + body
    // CMSG_PING (0x1DC): size = 8 (4-byte opcode + 4-byte sequence), body = sequence LE
    private static ByteBuf buildPingFrame(int sequence) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(8);     // size = 4 (opcode) + 4 (body)
        buf.writeByte(0xDC);   // opcode 0x1DC LE
        buf.writeByte(0x01);
        buf.writeByte(0x00);
        buf.writeByte(0x00);
        buf.writeIntLE(sequence);
        return buf;
    }
}