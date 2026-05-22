package com.example.world.infrastructure.netty.session;

import com.example.world.infrastructure.netty.crypto.CipherAttr;
import com.example.world.infrastructure.netty.crypto.WorldCipher;
import com.example.world.infrastructure.netty.protocol.packet.in.AuthSessionPacket;
import com.example.world.infrastructure.netty.protocol.packet.out.AddonInfoMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.AuthResponseMessage;
import com.example.world.service.WorldAccountService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler extends HandlerMap {

    private static final byte[] ZERO4 = new byte[4];

    private final WorldAccountService accountService;

    @PostConstruct
    void register() {
        on(AuthSessionPacket.class, WorldSession.State.AUTH_SESSION, this::handleAuthSession);
    }

    private void handleAuthSession(WorldSession session, AuthSessionPacket pkt) {
        log.debug("C_MSG_AUTH_SESSION: account='{}' build={}", pkt.account(), pkt.clientBuild());

        session.async(() -> {
            var acc = accountService.findAccountSession(pkt.account());
            if (acc == null || acc.sessionKey() == null) {
                log.warn("Account '{}' not found or session key missing", pkt.account());
                return () -> {
                    session.getCtx().writeAndFlush(new AuthResponseMessage(AuthResponseMessage.AUTH_UNKNOWN_ACCOUNT));
                    session.getCtx().close();
                };
            }
            if (!verifyDigest(pkt, acc.sessionKey(), session.getServerSeed())) {
                log.warn("Digest mismatch for account '{}'", pkt.account());
                return () -> {
                    session.getCtx().writeAndFlush(AuthResponseMessage.failed());
                    session.getCtx().close();
                };
            }
            WorldCipher cipher = new WorldCipher(acc.sessionKey());
            Long id = acc.id();
            return () -> {
                session.setAccountId(id);
                session.getCtx().channel().attr(CipherAttr.KEY).set(cipher);
                session.getCtx().write(new AddonInfoMessage(pkt.addonCount()));
                session.getCtx().write(AuthResponseMessage.ok());
                session.setState(WorldSession.State.IN_WORLD);
                session.getCtx().flush();
                log.info("Account '{}' (id={}) authenticated on world server", pkt.account(), id);
            };
        });
    }

    private boolean verifyDigest(AuthSessionPacket pkt, byte[] sessionKey, int serverSeed) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(pkt.account().getBytes(StandardCharsets.US_ASCII));
            sha1.update(ZERO4);
            sha1.update(intToLE(pkt.clientSeed()));
            sha1.update(intToLE(serverSeed));
            sha1.update(sessionKey);
            return Arrays.equals(sha1.digest(), pkt.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] intToLE(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
}