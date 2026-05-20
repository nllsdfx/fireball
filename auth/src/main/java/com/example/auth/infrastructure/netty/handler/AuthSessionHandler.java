package com.example.auth.infrastructure.netty.handler;

import com.example.auth.crypto.Srp6Challenge;
import com.example.auth.crypto.Srp6ProofResult;
import com.example.auth.crypto.Srp6Service;
import com.example.auth.infrastructure.netty.protocol.packet.in.LogonChallengePacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.LogonProofPacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.RealmListRequestPacket;
import com.example.auth.infrastructure.netty.protocol.packet.out.AuthResponse;
import com.example.auth.infrastructure.netty.protocol.packet.out.LogonChallengeResponse;
import com.example.auth.infrastructure.netty.protocol.packet.out.LogonProofResponse;
import com.example.auth.persistence.generated.tables.records.AccountRecord;
import com.example.auth.service.AccountService;
import com.example.auth.service.AuthErrorCode;
import com.example.auth.service.AuthProtocolException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Optional;

@Slf4j
public class AuthSessionHandler extends SimpleChannelInboundHandler<Object> {

    private static final int KEY_SIZE = 32;

    private enum State {CHALLENGE, PROOF, REALM_LIST, CLOSED}

    private final Srp6Service srp6Service;
    private final AccountService accountService;

    private State state = State.CHALLENGE;
    private String accountName;
    private Srp6Challenge srp6Challenge;

    public AuthSessionHandler(Srp6Service srp6Service, AccountService accountService) {
        this.srp6Service = srp6Service;
        this.accountService = accountService;
    }

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
        // Capture before spawning — field could theoretically change on event loop before vthread reads it
        String name = packet.accountName().toUpperCase(Locale.ROOT);
        accountName = name;
        log.debug("Logon challenge from account: {}", name);

        Thread.ofVirtual().start(() -> {
            try {
                AccountRecord account = accountService.findAccountByUserName(name);
                Srp6Challenge challenge = srp6Service.beginChallenge(account.getSalt(), account.getVerifier());
                LogonChallengeResponse response = new LogonChallengeResponse(
                        (byte) 0,
                        Srp6Service.toWire(challenge.serverPublicKey(), KEY_SIZE),
                        Srp6Service.GENERATOR,
                        Srp6Service.SAFE_PRIME_LE,
                        challenge.salt(),
                        srp6Service.generateCrcSalt(),
                        (byte) 0);
                // State and mutable fields must be written on the event loop to avoid races with incoming packets
                ctx.channel().eventLoop().execute(() -> {
                    srp6Challenge = challenge;
                    state = State.PROOF;
                    ctx.writeAndFlush(response);
                });
            } catch (Exception e) {
                ctx.channel().eventLoop().execute(() -> exceptionCaught(ctx, e));
            }
        });
    }

    private void handleProof(ChannelHandlerContext ctx, LogonProofPacket packet) {
        if (state != State.PROOF) {
            log.warn("Unexpected PROOF in state {}", state);
            ctx.close();
            return;
        }

        // SRP6 verification is pure CPU — runs on event loop (fast, non-blocking)
        Optional<Srp6ProofResult> result = srp6Service.verifyProof(
                srp6Challenge, accountName, packet.clientPublicKey(), packet.clientProof());

        if (result.isEmpty()) {
            throw new AuthProtocolException(AuthErrorCode.UNKNOWN_ACCOUNT, "SRP6 proof failed");
        }

        String name = accountName;
        Srp6ProofResult proof = result.get();

        Thread.ofVirtual().start(() -> {
            try {
                accountService.saveSessionKey(name, proof.sessionKey());
                log.debug("Auth success for account: {}", name);
                ctx.channel().eventLoop().execute(() -> {
                    state = State.REALM_LIST;
                    ctx.writeAndFlush(new LogonProofResponse((byte) 0, proof.serverProof(), 0));
                });
            } catch (Exception e) {
                ctx.channel().eventLoop().execute(() -> exceptionCaught(ctx, e));
            }
        });
    }

    private void handleRealmList(ChannelHandlerContext ctx, RealmListRequestPacket packet) {
        if (state != State.REALM_LIST) {
            log.warn("Unexpected REALM_LIST in state {}", state);
            ctx.close();
            return;
        }
        // TODO: load realms from auth.realm via RealmRepository, send RealmListResponse
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        state = State.CLOSED;
        log.debug("Client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof AuthProtocolException e) {
            log.debug("Auth protocol error for {}: {}", accountName, e.getMessage());
            AuthResponse response = switch (state) {
                case CHALLENGE -> LogonChallengeResponse.error(e.errorCode().code());
                case PROOF     -> LogonProofResponse.error(e.errorCode().code());
                default        -> null;
            };
            if (response != null) ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            else ctx.close();
        } else {
            log.error("Unexpected auth error for {}", accountName, cause);
            ctx.close();
        }
    }
}
