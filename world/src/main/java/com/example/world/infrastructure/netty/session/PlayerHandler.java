package com.example.world.infrastructure.netty.session;

import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.infrastructure.netty.protocol.packet.in.PlayerLoginRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.AccountDataTimesMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.LoginSetTimeSpeedMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.LoginVerifyWorldMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.TutorialFlagsMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class PlayerHandler extends HandlerMap {

    private final CharacterUseCase characterUseCase;

    @PostConstruct
    void register() {
        on(PlayerLoginRequest.class, WorldSession.State.IN_WORLD, this::handlePlayerLogin);
    }

    private void handlePlayerLogin(WorldSession session, PlayerLoginRequest req) {
        session.async(() -> {
            var character = characterUseCase.getCharacter(req.guid(), session.getAccountId());
            if (character.isEmpty()) {
                log.warn("PLAYER_LOGIN: guid={} not found for accountId={}", req.guid(), session.getAccountId());
                return () -> session.getCtx().close();
            }
            var pos = character.get().getPosition();
            return () -> {
                var ctx = session.getCtx();
                ctx.write(new LoginVerifyWorldMessage(pos.getMapId(), pos.getX(), pos.getY(), pos.getZ(), pos.getOrientation()));
                ctx.write(new TutorialFlagsMessage());
                ctx.write(new AccountDataTimesMessage());
                ctx.writeAndFlush(new LoginSetTimeSpeedMessage());
                log.info("PLAYER_LOGIN: character '{}' (guid={}) entered world at map={} ({},{},{})",
                        character.get().getName(), req.guid(), pos.getMapId(),
                        pos.getX(), pos.getY(), pos.getZ());
            };
        });
    }
}