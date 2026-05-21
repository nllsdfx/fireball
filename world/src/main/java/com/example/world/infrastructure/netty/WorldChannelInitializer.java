package com.example.world.infrastructure.netty;

import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.infrastructure.netty.handler.WorldSessionHandler;
import com.example.world.infrastructure.netty.protocol.WorldPacketDecoder;
import com.example.world.infrastructure.netty.protocol.WorldPacketEncoder;
import com.example.world.service.WorldAccountService;
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
    private final WorldAccountService accountService;
    private final CharacterUseCase characterUseCase;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                .addLast(encoder)
                .addLast(new WorldPacketDecoder())
                .addLast(new WorldSessionHandler(accountService, characterUseCase));
    }
}
