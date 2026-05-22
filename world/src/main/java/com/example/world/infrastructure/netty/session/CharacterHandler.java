package com.example.world.infrastructure.netty.session;

import com.example.world.domain.model.Character;
import com.example.world.domain.model.CharClass;
import com.example.world.domain.model.Gender;
import com.example.world.domain.model.Race;
import com.example.world.domain.model.RaceStartPosition;
import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.infrastructure.netty.protocol.packet.in.CharCreateRequest;
import com.example.world.infrastructure.netty.protocol.packet.in.CharDeleteRequest;
import com.example.world.infrastructure.netty.protocol.packet.in.CharEnumRequest;
import com.example.world.infrastructure.netty.protocol.packet.out.CharCreateResponse;
import com.example.world.infrastructure.netty.protocol.packet.out.CharDeleteResponse;
import com.example.world.infrastructure.netty.protocol.packet.out.CharEnumResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterHandler extends HandlerMap {

    private final CharacterUseCase characterUseCase;

    @PostConstruct
    void register() {
        on(CharCreateRequest.class, WorldSession.State.IN_WORLD, this::handleCharCreate);
        on(CharDeleteRequest.class, WorldSession.State.IN_WORLD, this::handleCharDelete);
        on(CharEnumRequest.class,   WorldSession.State.IN_WORLD, this::handleCharEnum);
    }

    private void handleCharCreate(WorldSession session, CharCreateRequest req) {
        Race race         = Race.fromId(req.race());
        CharClass cls     = CharClass.fromId(req.charClass());
        Gender gender     = Gender.fromId(req.gender());

        if (race == null || cls == null || gender == null) {
            session.getCtx().writeAndFlush(new CharCreateResponse(CharCreateResponse.Result.ERROR));
            return;
        }

        var start = RaceStartPosition.forRace(race);
        var character = new Character(null, session.getAccountId(), req.name(),
                race, cls, gender,
                req.skin(), req.face(), req.hairStyle(), req.hairColor(), req.facialHair(),
                (short) 1, start.getZone(), start.toPosition());

        session.async(() -> {
            try {
                characterUseCase.createCharacter(character);
                return () -> session.getCtx().writeAndFlush(new CharCreateResponse(CharCreateResponse.Result.SUCCESS));
            } catch (DataAccessException e) {
                return () -> session.getCtx().writeAndFlush(new CharCreateResponse(CharCreateResponse.Result.NAME_IN_USE));
            }
        });
    }

    private void handleCharDelete(WorldSession session, CharDeleteRequest req) {
        session.async(() -> {
            boolean deleted = characterUseCase.deleteCharacter(req.guid(), session.getAccountId());
            var result = deleted ? CharDeleteResponse.Result.SUCCESS : CharDeleteResponse.Result.FAILED;
            return () -> session.getCtx().writeAndFlush(new CharDeleteResponse(result));
        });
    }

    private void handleCharEnum(WorldSession session, CharEnumRequest ignored) {
        session.async(() -> {
            var chars = characterUseCase.listCharacters(session.getAccountId());
            return () -> session.getCtx().writeAndFlush(new CharEnumResponse(chars));
        });
    }
}