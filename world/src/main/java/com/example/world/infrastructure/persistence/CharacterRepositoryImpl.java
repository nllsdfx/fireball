package com.example.world.infrastructure.persistence;

import com.example.world.domain.model.*;
import com.example.world.domain.model.Character;
import com.example.world.domain.port.out.CharacterRepository;
import com.example.world.persistence.generated.tables.records.CharacterRecord;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.world.persistence.generated.Tables.CHARACTER;

@Repository
@RequiredArgsConstructor
public class CharacterRepositoryImpl implements CharacterRepository {

    private final DSLContext dsl;

    @Override
    public void create(@NonNull Character character) {
        var pos = character.getPosition();
        dsl.insertInto(CHARACTER)
                .set(CHARACTER.ACCOUNT_ID,  character.getAccountId())
                .set(CHARACTER.NAME,        character.getName())
                .set(CHARACTER.RACE,        character.getRace().getId())
                .set(CHARACTER.CHAR_CLASS,  character.getCharClass().getId())
                .set(CHARACTER.GENDER,      character.getGender().getId())
                .set(CHARACTER.SKIN,        character.getSkin())
                .set(CHARACTER.FACE,        character.getFace())
                .set(CHARACTER.HAIR_STYLE,  character.getHairStyle())
                .set(CHARACTER.HAIR_COLOR,  character.getHairColor())
                .set(CHARACTER.FACIAL_HAIR, character.getFacialHair())
                .set(CHARACTER.LEVEL,       character.getLevel())
                .set(CHARACTER.ZONE,        character.getZone())
                .set(CHARACTER.MAP_ID,      pos.getMapId())
                .set(CHARACTER.POS_X,       pos.getX())
                .set(CHARACTER.POS_Y,       pos.getY())
                .set(CHARACTER.POS_Z,       pos.getZ())
                .set(CHARACTER.ORIENTATION, pos.getOrientation())
                .execute();
    }

    @Override
    public @NonNull List<Character> findByAccountId(@NonNull Long accountId) {
        return dsl.selectFrom(CHARACTER)
                .where(CHARACTER.ACCOUNT_ID.eq(accountId))
                .fetch(this::toCharacter);
    }

    private Character toCharacter(CharacterRecord r) {
        var position = new Position(
                r.getMapId(),
                r.getPosX(),
                r.getPosY(),
                r.getPosZ(),
                r.getOrientation()
        );
        return new Character(
                r.getId(),
                r.getAccountId(),
                r.getName(),
                Race.fromId(r.getRace()),
                CharClass.fromId(r.getCharClass()),
                Gender.fromId(r.getGender()),
                r.getSkin(),
                r.getFace(),
                r.getHairStyle(),
                r.getHairColor(),
                r.getFacialHair(),
                r.getLevel(),
                r.getZone(),
                position
        );
    }
}