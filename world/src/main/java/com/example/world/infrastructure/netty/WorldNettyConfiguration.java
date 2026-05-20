package com.example.world.infrastructure.netty;

import com.example.shared.netty.NettyServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorldNettyConfiguration {

    @Bean
    public NettyServer worldNettyServer(WorldChannelInitializer initializer, WorldNettyProperties properties) {
        return new NettyServer(initializer, properties.getPort(), properties.getBossThreads(), properties.getWorkerThreads());
    }
}
