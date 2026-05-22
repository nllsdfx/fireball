package com.example.world.infrastructure.netty.session;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class WorldSession {

    private static final int MAX_TASKS_PER_TICK = 100;
    private static final Executor VIRTUAL = Executors.newVirtualThreadPerTaskExecutor();

    public enum State { AUTH_SESSION, IN_WORLD, CLOSED }

    private final HandlerRegistry registry;

    @Getter @Setter private ChannelHandlerContext ctx;
    @Getter @Setter private int serverSeed;
    @Getter @Setter private volatile State state = State.AUTH_SESSION;
    @Getter @Setter private volatile Long accountId;

    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    public void enqueue(Runnable task) {
        taskQueue.add(task);
    }

    public void tick(long diffMillis) {
        int processed = 0;
        Runnable task;
        while (processed < MAX_TASKS_PER_TICK && (task = taskQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Error in session task: {}", e.getMessage(), e);
            }
            processed++;
        }
    }

    public void dispatch(Object msg) {
        registry.dispatch(this, msg);
    }

    public void async(Supplier<Runnable> task) {
        CompletableFuture.supplyAsync(task, VIRTUAL)
                .thenAccept(this::enqueue)
                .exceptionally(e -> {
                    enqueue(() -> { log.error("Session error: {}", e.getMessage(), e); ctx.close(); });
                    return null;
                });
    }
}