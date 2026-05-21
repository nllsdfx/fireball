package com.example.world.infrastructure.netty.session;

import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.infrastructure.netty.crypto.CipherAttr;
import com.example.world.infrastructure.netty.crypto.WorldCipher;
import com.example.world.infrastructure.netty.protocol.packet.in.AuthSessionPacket;
import com.example.world.infrastructure.netty.protocol.packet.in.CharEnumRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.AddonInfoMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthResponseMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.CharEnumResponse;
import com.example.world.service.WorldAccountService;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RequiredArgsConstructor
public class WorldSession {

    private static final int MAX_TASKS_PER_TICK = 100;
    private static final byte[] ZERO4 = new byte[4];
    private static final MessageDigest SHA1_PROTO;

    static {
        try {
            SHA1_PROTO = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public enum State { AUTH_SESSION, IN_WORLD, CLOSED }

    private final WorldAccountService accountService;
    private final CharacterUseCase characterUseCase;

    @Getter
    @Setter
    private ChannelHandlerContext ctx;
    private State state = State.AUTH_SESSION;
    @Getter
    @Setter
    private int serverSeed;
    private Long accountId;

    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    public void enqueue(Runnable task) {
        taskQueue.add(task);
    }

    public void tick(long diffMillis) {
        int processed = 0;
        Runnable task;
        while (processed < MAX_TASKS_PER_TICK && (task = taskQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Error in session task: {}", e.getMessage(), e);
            }
            processed++;
        }
    }

    public void dispatch(Object msg) {
        switch (msg) {
            case AuthSessionPacket pkt -> handleAuthSession(pkt);
            case CharEnumRequest ignored -> handleCharEnum();
            default -> {
                log.warn("Unexpected message type {}, closing", msg.getClass().getSimpleName());
                ctx.close();
            }
        }
    }

    private void handleAuthSession(AuthSessionPacket pkt) {
        if (state != State.AUTH_SESSION) {
            log.warn("C_MSG_AUTH_SESSION in wrong state {}, closing", state);
            ctx.close();
            return;
        }
        log.debug("C_MSG_AUTH_SESSION: account='{}' build={}", pkt.account(), pkt.clientBuild());

        Thread.ofVirtual().start(() -> {
            try {
                var session = accountService.findAccountSession(pkt.account());
                if (session == null || session.sessionKey() == null) {
                    log.warn("Account '{}' not found or session key missing", pkt.account());
                    enqueue(() -> {
                        ctx.writeAndFlush(new AuthResponseMessage(AuthResponseMessage.AUTH_UNKNOWN_ACCOUNT));
                        ctx.close();
                    });
                    return;
                }
                if (!verifyDigest(pkt, session.sessionKey())) {
                    log.warn("Digest mismatch for account '{}'", pkt.account());
                    enqueue(() -> {
                        ctx.writeAndFlush(AuthResponseMessage.failed());
                        ctx.close();
                    });
                    return;
                }
                WorldCipher cipher = new WorldCipher(session.sessionKey());
                Long id = session.id();
                enqueue(() -> {
                    accountId = id;
                    ctx.channel().attr(CipherAttr.KEY).set(cipher);
                    ctx.write(new AddonInfoMessage(pkt.addonCount()));
                    ctx.writeAndFlush(AuthResponseMessage.ok());
                    state = State.IN_WORLD;
                    log.info("Account '{}' (id={}) authenticated on world server", pkt.account(), accountId);
                });
            } catch (Exception e) {
                enqueue(() -> {
                    log.error("Auth session error: {}", e.getMessage(), e);
                    ctx.close();
                });
            }
        });
    }

    private void handleCharEnum() {
        if (state != State.IN_WORLD) {
            log.warn("CMSG_CHAR_ENUM in wrong state {}, closing", state);
            ctx.close();
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                var characters = characterUseCase.listCharacters(accountId);
                enqueue(() -> ctx.writeAndFlush(new CharEnumResponse(characters)));
            } catch (Exception e) {
                enqueue(() -> {
                    log.error("Char enum error: {}", e.getMessage(), e);
                    ctx.close();
                });
            }
        });
    }

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

    private static byte[] intToLE(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
}