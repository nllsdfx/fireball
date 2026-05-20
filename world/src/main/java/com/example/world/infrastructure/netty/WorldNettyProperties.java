package com.example.world.infrastructure.netty;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "netty")
public class WorldNettyProperties {

    private int port = 8085;
    private int bossThreads = 1;
    private int workerThreads = 0;
}
