package com.example.world.infrastructure.netty.handler;

import com.example.world.infrastructure.netty.crypto.CipherAttr;
import com.example.world.infrastructure.netty.crypto.WorldCipher;
import com.example.world.infrastructure.netty.protocol.packet.in.AuthSessionPacket;
import com.example.world.infrastructure.netty.protocol.packet.out.AddonInfoMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthChallengeMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthResponseMessage;
import com.example.world.service.WorldAccountService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class WorldSessionHandler extends SimpleChannelInboundHandler<Object> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final byte[] ZERO4 = new byte[4];
    private static final MessageDigest SHA1_PROTO;
    static {
        try {
            SHA1_PROTO = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private enum State { AUTH_SESSION, IN_WORLD, CLOSED }

    private final WorldAccountService accountService;

    private State state = State.AUTH_SESSION;
    private int serverSeed;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        serverSeed = SECURE_RANDOM.nextInt();
        state = State.AUTH_SESSION;
        ctx.writeAndFlush(new AuthChallengeMessage(serverSeed));
        log.debug("World connection from {}, seed=0x{}", ctx.channel().remoteAddress(), Integer.toHexString(serverSeed));
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
            log.warn("C_MSG_AUTH_SESSION in wrong state {}, closing", state);
            ctx.close();
            return;
        }
        log.debug("C_MSG_AUTH_SESSION: account='{}' build={}", pkt.account(), pkt.clientBuild());

        Thread.ofVirtual().start(() -> {
            try {
                byte[] sessionKey = accountService.findSessionKey(pkt.account());
                if (sessionKey == null) {
                    log.warn("Account '{}' not found or session key missing", pkt.account());
                    rejectOnEventLoop(ctx, AuthResponseMessage.AUTH_UNKNOWN_ACCOUNT);
                    return;
                }

                if (!verifyDigest(pkt, sessionKey)) {
                    log.warn("Digest mismatch for account '{}'", pkt.account());
                    rejectOnEventLoop(ctx, AuthResponseMessage.AUTH_FAILED);
                    return;
                }

                WorldCipher cipher = new WorldCipher(sessionKey);

                ctx.channel().eventLoop().execute(() -> {
                    ctx.channel().attr(CipherAttr.KEY).set(cipher);
                    ctx.write(new AddonInfoMessage(pkt.addonCount()));
                    ctx.writeAndFlush(AuthResponseMessage.ok());
                    state = State.IN_WORLD;
                    log.info("Account '{}' authenticated on world server", pkt.account());
                });
            } catch (Exception e) {
                ctx.channel().eventLoop().execute(() -> exceptionCaught(ctx, e));
            }
        });
    }

    /**
     * Replicates cmangos HandleAuthSession digest check:
     * SHA1(account + [0x00000000] + clientSeed_LE + serverSeed_LE + sessionKey_LE)
     */
    private boolean verifyDigest(AuthSessionPacket pkt, byte[] sessionKey) {
        try {
            MessageDigest sha1 = (MessageDigest) SHA1_PROTO.clone();
            sha1.update(pkt.account().getBytes(StandardCharsets.US_ASCII));
            sha1.update(ZERO4);
            sha1.update(intToLE(pkt.clientSeed()));
            sha1.update(intToLE(serverSeed));
            sha1.update(sessionKey);
            return Arrays.equals(sha1.digest(), pkt.digest());
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void rejectOnEventLoop(ChannelHandlerContext ctx, byte errorCode) {
        ctx.channel().eventLoop().execute(() -> {
            ctx.writeAndFlush(new AuthResponseMessage(errorCode));
            ctx.close();
        });
    }

    private static byte[] intToLE(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("World session error from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
