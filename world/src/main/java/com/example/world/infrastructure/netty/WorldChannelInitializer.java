package com.example.world.infrastructure.netty;

import com.example.world.infrastructure.netty.handler.WorldSessionHandler;
import com.example.world.infrastructure.netty.protocol.WorldPacketDecoder;
import com.example.world.infrastructure.netty.protocol.WorldPacketEncoder;
import com.example.world.infrastructure.netty.session.HandlerRegistry;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorldChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final WorldPacketEncoder encoder;
    private final HandlerRegistry registry;
    private final WorldUpdateLoop updateLoop;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                .addLast(encoder)
                .addLast(new WorldPacketDecoder())
                .addLast(new WorldSessionHandler(registry, updateLoop));
    }
}
