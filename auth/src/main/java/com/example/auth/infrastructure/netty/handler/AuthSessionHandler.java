package com.example.auth.infrastructure.netty.handler;

import com.example.auth.infrastructure.netty.protocol.packet.in.LogonChallengePacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.LogonProofPacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.RealmListRequestPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthSessionHandler extends SimpleChannelInboundHandler<Object> {

    private enum State {CHALLENGE, PROOF, REALM_LIST, CLOSED}

    private State state = State.CHALLENGE;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        switch (msg) {
            case LogonChallengePacket p -> handleChallenge(ctx, p);
            case LogonProofPacket p -> handleProof(ctx, p);
            case RealmListRequestPacket p -> handleRealmList(ctx, p);
            default -> {
                log.warn("Unexpected packet type: {}", msg.getClass().getSimpleName());
                ctx.close();
            }
        }
    }

    private void handleChallenge(ChannelHandlerContext ctx, LogonChallengePacket packet) {
        if (state != State.CHALLENGE) {
            log.warn("Unexpected CHALLENGE in state {}", state);
            ctx.close();
            return;
        }
        log.debug("Logon challenge from account: {}", packet.accountName());
        // SRP6 challenge logic goes here
        state = State.PROOF;
    }

    private void handleProof(ChannelHandlerContext ctx, LogonProofPacket packet) {
        if (state != State.PROOF) {
            log.warn("Unexpected PROOF in state {}", state);
            ctx.close();
            return;
        }
        // SRP6 proof verification goes here
        state = State.REALM_LIST;
    }

    private void handleRealmList(ChannelHandlerContext ctx, RealmListRequestPacket packet) {
        if (state != State.REALM_LIST) {
            log.warn("Unexpected REALM_LIST in state {}", state);
            ctx.close();
            return;
        }
        // Realm list fetch and response goes here
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        state = State.CLOSED;
        log.debug("Client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Auth session error", cause);
        ctx.close();
    }
}
