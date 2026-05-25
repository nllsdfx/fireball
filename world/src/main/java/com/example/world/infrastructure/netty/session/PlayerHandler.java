package com.example.world.infrastructure.netty.session;

import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.infrastructure.netty.protocol.packet.in.PlayerLoginRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.AccountDataTimesMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.LoginSetTimeSpeedMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.LoginVerifyWorldMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.TutorialFlagsMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.ActionButtonsMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.InitWorldStatesMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.InitializeFactionMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.InitialSpellsMessage;
import com.example.world.infrastructure.netty.protocol.packet.out.UpdateObjectMessage;
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
            var chr = character.get();
            var pos = chr.getPosition();
            return () -> {
                var ctx = session.getCtx();
                ctx.write(new LoginVerifyWorldMessage(pos.getMapId(), pos.getX(), pos.getY(), pos.getZ(), pos.getOrientation()));
                ctx.write(new TutorialFlagsMessage());
                ctx.write(new AccountDataTimesMessage());
                ctx.write(new LoginSetTimeSpeedMessage());
                ctx.write(new InitialSpellsMessage());
                ctx.write(new ActionButtonsMessage());
                ctx.write(new InitializeFactionMessage());
                ctx.write(new UpdateObjectMessage(chr));
                ctx.writeAndFlush(new InitWorldStatesMessage(pos.getMapId(), chr.getZone()));
                log.info("PLAYER_LOGIN: character '{}' (guid={}) entered world at map={} ({},{},{})",
                        chr.getName(), req.guid(), pos.getMapId(), pos.getX(), pos.getY(), pos.getZ());
            };
        });
    }
}