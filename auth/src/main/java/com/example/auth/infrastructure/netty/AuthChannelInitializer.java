package com.example.auth.infrastructure.netty;

import com.example.auth.infrastructure.netty.handler.AuthSessionHandler;
import com.example.auth.infrastructure.netty.protocol.AuthPacketDecoder;
import com.example.auth.infrastructure.netty.protocol.AuthPacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final AuthPacketEncoder encoder;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(encoder)
                .addLast(new AuthPacketDecoder())
                .addLast(new AuthSessionHandler());
    }
}
