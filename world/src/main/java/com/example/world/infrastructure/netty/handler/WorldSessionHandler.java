package com.example.world.infrastructure.netty.handler;

import com.example.world.infrastructure.netty.WorldUpdateLoop;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthChallengeMessage;
import com.example.world.infrastructure.netty.session.HandlerRegistry;
import com.example.world.infrastructure.netty.session.WorldSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

@Slf4j
@RequiredArgsConstructor
public class WorldSessionHandler extends SimpleChannelInboundHandler<Object> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final HandlerRegistry registry;
    private final WorldUpdateLoop updateLoop;

    private WorldSession session;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        session = new WorldSession(registry);
        session.setCtx(ctx);
        session.setServerSeed(SECURE_RANDOM.nextInt());
        ctx.writeAndFlush(new AuthChallengeMessage(session.getServerSeed()));
        updateLoop.register(session);
        log.debug("World connection from {}, seed=0x{}", ctx.channel().remoteAddress(), Integer.toHexString(session.getServerSeed()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        session.enqueue(() -> session.dispatch(msg));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (session != null) updateLoop.unregister(ctx.channel().id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("World session error from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        if (session != null) updateLoop.unregister(ctx.channel().id());
        ctx.close();
    }
}
