package com.example.world.infrastructure.netty.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HandlerRegistry {

    private final Map<Class<?>, DomainHandler> routing;

    public HandlerRegistry(List<DomainHandler> handlers) {
        Map<Class<?>, DomainHandler> map = new HashMap<>();
        handlers.forEach(h -> h.packetTypes().forEach(type -> map.put(type, h)));
        routing = Collections.unmodifiableMap(map);
        log.info("HandlerRegistry: {} packet types registered", routing.size());
    }

    public void dispatch(WorldSession session, Object msg) {
        DomainHandler handler = routing.get(msg.getClass());
        if (handler == null) {
            log.warn("No handler for {}, closing", msg.getClass().getSimpleName());
            session.getCtx().close();
            return;
        }
        handler.dispatch(session, msg);
    }
}