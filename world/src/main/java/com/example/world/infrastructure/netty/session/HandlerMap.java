package com.example.world.infrastructure.netty.session;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
public class HandlerMap implements DomainHandler {

    private record Entry(WorldSession.State requiredState, BiConsumer<WorldSession, Object> handler) {}

    private final Map<Class<?>, Entry> handlers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void on(Class<T> type, WorldSession.State requiredState, BiConsumer<WorldSession, T> handler) {
        handlers.put(type, new Entry(requiredState, (s, msg) -> handler.accept(s, (T) msg)));
    }

    @Override
    public Set<Class<?>> packetTypes() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    @Override
    public void dispatch(WorldSession session, Object msg) {
        Entry entry = handlers.get(msg.getClass());
        if (entry == null) {
            log.warn("No entry for {} in {}, closing", msg.getClass().getSimpleName(), getClass().getSimpleName());
            session.getCtx().close();
            return;
        }
        if (session.getState() != entry.requiredState()) {
            log.warn("Packet {} rejected: state={}, required={} — dropping",
                    msg.getClass().getSimpleName(), session.getState(), entry.requiredState());
            return;
        }
        entry.handler().accept(session, msg);
    }
}