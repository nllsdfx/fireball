package com.example.world.infrastructure.netty;

import com.example.world.infrastructure.netty.session.WorldSession;
import io.netty.channel.ChannelId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class WorldUpdateLoop implements SmartLifecycle {

    private final ConcurrentHashMap<ChannelId, WorldSession> sessions = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;
    private long lastUpdateTime;
    private volatile boolean running;

    public void register(WorldSession session) {
        sessions.put(session.getCtx().channel().id(), session);
    }

    public void unregister(ChannelId id) {
        sessions.remove(id);
    }

    @Override
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        lastUpdateTime = System.currentTimeMillis();
        updateTask = scheduler.scheduleAtFixedRate(this::update, 0, 100, TimeUnit.MILLISECONDS);
        running = true;
        log.info("WorldUpdateLoop started");
    }

    @Override
    public void stop() {
        running = false;
        if (updateTask != null) updateTask.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
        log.info("WorldUpdateLoop stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void update() {
        long now = System.currentTimeMillis();
        long diff = now - lastUpdateTime;
        lastUpdateTime = now;
        for (WorldSession session : sessions.values()) {
            try {
                session.tick(diff);
            } catch (Exception e) {
                log.error("Error ticking session: {}", e.getMessage(), e);
            }
        }
    }
}
