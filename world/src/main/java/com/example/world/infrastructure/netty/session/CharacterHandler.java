package com.example.world.infrastructure.netty.session;

import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.infrastructure.netty.protocol.packet.in.CharEnumRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.CharEnumResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterHandler extends HandlerMap {

    private final CharacterUseCase characterUseCase;

    @PostConstruct
    void register() {
        on(CharEnumRequest.class, WorldSession.State.IN_WORLD, this::handleCharEnum);
    }

    private void handleCharEnum(WorldSession session, CharEnumRequest ignored) {
        session.async(() -> {
            var chars = characterUseCase.listCharacters(session.getAccountId());
            return () -> session.getCtx().writeAndFlush(new CharEnumResponse(chars));
        });
    }
}