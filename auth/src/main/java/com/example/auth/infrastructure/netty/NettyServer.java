package com.example.auth.infrastructure.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NettyServer implements SmartLifecycle {

    private final AuthChannelInitializer initializer;
    private final NettyProperties properties;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private volatile boolean running;

    @Override
    public void start() {
        bossGroup = new MultiThreadIoEventLoopGroup(properties.getBossThreads(), NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(properties.getWorkerThreads(), NioIoHandler.newFactory());

        try {
            channelFuture = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer)
                    .bind(properties.getPort())
                    .sync();
            running = true;
            log.info("Netty server started on port {}", properties.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (channelFuture != null) {
            channelFuture.channel().close().awaitUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("Netty server stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
