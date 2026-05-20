package com.example.auth.infrastructure.netty;

import com.example.auth.crypto.Srp6Service;
import com.example.auth.infrastructure.netty.handler.AuthSessionHandler;
import com.example.auth.infrastructure.netty.protocol.AuthPacketDecoder;
import com.example.auth.infrastructure.netty.protocol.AuthPacketEncoder;
import com.example.auth.service.AccountService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final AuthPacketEncoder encoder;
    private final Srp6Service srp6Service;
    private final AccountService accountService;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(encoder)
                .addLast(new AuthPacketDecoder())
                .addLast(new AuthSessionHandler(srp6Service, accountService));
    }
}
