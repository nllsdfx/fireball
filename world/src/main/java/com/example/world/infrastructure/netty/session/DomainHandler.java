package com.example.world.infrastructure.netty.session;

import java.util.Set;

public interface DomainHandler {
    Set<Class<?>> packetTypes();
    void dispatch(WorldSession session, Object packet);
}