package com.example.world.infrastructure.netty.session;

import com.example.world.infrastructure.netty.protocol.packet.in.PingRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.PongMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class WorldHandler extends HandlerMap {

    @PostConstruct
    void register() {
        on(PingRequest.class, WorldSession.State.IN_WORLD, this::handlePing);
    }

    private void handlePing(WorldSession session, PingRequest pkt) {
        session.getCtx().writeAndFlush(new PongMessage(pkt.sequence()));
    }
}