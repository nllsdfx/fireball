package com.example.auth.infrastructure.netty;

import com.example.auth.crypto.Srp6Service;
import com.example.auth.infrastructure.netty.handler.AuthSessionHandler;
import com.example.auth.infrastructure.netty.protocol.AuthPacketDecoder;
import com.example.auth.infrastructure.netty.protocol.AuthPacketEncoder;
import com.example.auth.persistence.repository.RealmRepository;
import com.example.auth.service.AccountService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final AuthPacketEncoder encoder;
    private final Srp6Service srp6Service;
    private final AccountService accountService;
    private final RealmRepository realmRepository;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                .addLast(encoder)
                .addLast(new AuthPacketDecoder())
                .addLast(new AuthSessionHandler(srp6Service, accountService, realmRepository));
    }
}
