package com.example.shared.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

@Slf4j
public class NettyServer implements SmartLifecycle {

    private final ChannelInitializer<SocketChannel> initializer;
    private final int port;
    private final int bossThreads;
    private final int workerThreads;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private volatile boolean running;

    public NettyServer(ChannelInitializer<SocketChannel> initializer, int port, int bossThreads, int workerThreads) {
        this.initializer = initializer;
        this.port = port;
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads;
    }

    @Override
    public void start() {
        bossGroup = new MultiThreadIoEventLoopGroup(bossThreads, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(workerThreads, NioIoHandler.newFactory());
        try {
            channelFuture = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer)
                    .bind(port)
                    .sync();
            running = true;
            log.info("Netty server started on port {}", port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (channelFuture != null) channelFuture.channel().close().awaitUninterruptibly();
        if (bossGroup != null) bossGroup.shutdownGracefully().awaitUninterruptibly();
        if (workerGroup != null) workerGroup.shutdownGracefully().awaitUninterruptibly();
        log.info("Netty server stopped on port {}.", port);
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
