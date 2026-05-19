package com.example.auth.infrastructure.netty;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "netty")
public class NettyProperties {

    private int port = 3724;
    private int bossThreads = 1;
    private int workerThreads = 0; // 0 = Netty default (2 * CPU cores)
}
