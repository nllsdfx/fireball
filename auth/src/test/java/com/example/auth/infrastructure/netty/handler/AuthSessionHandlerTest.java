package com.example.auth.infrastructure.netty.handler;

import com.example.auth.crypto.Srp6Service;
import com.example.auth.infrastructure.netty.protocol.packet.in.LogonProofPacket;
import com.example.auth.infrastructure.netty.protocol.packet.in.RealmListRequestPacket;
import com.example.auth.infrastructure.netty.protocol.packet.out.LogonChallengeResponse;
import com.example.auth.infrastructure.netty.protocol.packet.out.LogonProofResponse;
import com.example.auth.persistence.repository.RealmRepository;
import com.example.auth.service.AccountService;
import com.example.auth.service.AuthErrorCode;
import com.example.auth.service.AuthProtocolException;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthSessionHandlerTest {

    private final Srp6Service srp6Service       = new Srp6Service();
    private final AccountService accountService  = mock(AccountService.class);
    private final RealmRepository realmRepository = mock(RealmRepository.class);

    private AuthSessionHandler handler;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        handler = new AuthSessionHandler(srp6Service, accountService, realmRepository);
        channel = new EmbeddedChannel(handler);
    }

    @AfterEach
    void tearDown() {
        channel.finishAndReleaseAll();
    }

    // --- wrong-state guards (synchronous, no virtual thread involved) ---

    @Test
    void proofPacketInChallengeState_closesChannel() {
        var packet = new LogonProofPacket(new byte[32], new byte[20], new byte[20], (byte) 0, (byte) 0);
        channel.writeInbound(packet);
        assertThat(channel.isActive()).isFalse();
    }

    @Test
    void realmListPacketInChallengeState_closesChannel() {
        channel.writeInbound(new RealmListRequestPacket(0));
        assertThat(channel.isActive()).isFalse();
    }

    @Test
    void unknownPacketType_closesChannel() {
        channel.writeInbound("unexpected");
        assertThat(channel.isActive()).isFalse();
    }

    // --- exceptionCaught ---

    @Test
    void exceptionCaught_authProtocolInChallengeState_writesErrorResponseAndCloses() {
        var ex = new AuthProtocolException(AuthErrorCode.UNKNOWN_ACCOUNT, "test");
        channel.pipeline().fireExceptionCaught(ex);

        // Must write a LogonChallengeResponse error (state=CHALLENGE at construction)
        Object written = channel.readOutbound();
        assertThat(written).isInstanceOf(LogonChallengeResponse.class);
        assertThat(((LogonChallengeResponse) written).error())
                .isEqualTo(AuthErrorCode.UNKNOWN_ACCOUNT.code());
        assertThat(channel.isActive()).isFalse();
    }

    @Test
    void exceptionCaught_genericException_closesChannelWithoutResponse() {
        channel.pipeline().fireExceptionCaught(new RuntimeException("boom"));
        assertThat(channel.<Object>readOutbound()).isNull();
        assertThat(channel.isActive()).isFalse();
    }
}
