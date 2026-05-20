package com.example.auth.infrastructure.netty;

import com.example.shared.netty.NettyServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfiguration {

    @Bean
    public NettyServer authNettyServer(AuthChannelInitializer initializer, NettyProperties properties) {
        return new NettyServer(initializer, properties.getPort(), properties.getBossThreads(), properties.getWorkerThreads());
    }
}
