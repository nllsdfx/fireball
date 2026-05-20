package com.example.world.infrastructure.netty.handler;

import com.example.world.infrastructure.netty.protocol.packet.in.AuthSessionPacket;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthChallengeMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

@Slf4j
public class WorldSessionHandler extends SimpleChannelInboundHandler<Object> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private enum State {
        AUTH_CHALLENGE, AUTH_SESSION, IN_WORLD, CLOSED
    }

    private State state = State.AUTH_CHALLENGE;
    private int serverSeed;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        serverSeed = SECURE_RANDOM.nextInt();
        state = State.AUTH_SESSION;
        ctx.writeAndFlush(new AuthChallengeMessage(serverSeed));
        log.debug("New world connection from {}, sent AUTH_CHALLENGE seed=0x{}",
                ctx.channel().remoteAddress(), Integer.toHexString(serverSeed));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        switch (msg) {
            case AuthSessionPacket pkt -> handleAuthSession(ctx, pkt);
            default -> {
                log.warn("Unexpected message type {}, closing", msg.getClass().getSimpleName());
                ctx.close();
            }
        }
    }

    private void handleAuthSession(ChannelHandlerContext ctx, AuthSessionPacket pkt) {
        if (state != State.AUTH_SESSION) {
            log.warn("CMSG_AUTH_SESSION in wrong state {}, closing", state);
            ctx.close();
            return;
        }
        log.info("CMSG_AUTH_SESSION: account='{}' build={} clientSeed=0x{}",
                pkt.account(), pkt.clientBuild(), Integer.toHexString(pkt.clientSeed()));
        // TODO step 2: verify digest against session key from DB, init encryption, send SMSG_AUTH_RESPONSE
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("World session error from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
