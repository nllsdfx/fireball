package com.example.world.infrastructure.persistence;

import com.example.world.domain.model.CharClass;
import com.example.world.domain.model.Character;
import com.example.world.domain.model.Gender;
import com.example.world.domain.model.Position;
import com.example.world.domain.model.Race;
import com.example.world.domain.port.out.CharacterRepository;
import com.example.world.persistence.generated.tables.records.CharacterRecord;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.world.persistence.generated.Tables.CHARACTER;

@Repository
@RequiredArgsConstructor
public class CharacterRepositoryImpl implements CharacterRepository {

    private static final Map<Integer, Race> RACE_BY_ID = Arrays.stream(Race.values())
            .collect(Collectors.toMap(Race::getId, Function.identity()));
    private static final Map<Integer, CharClass> CLASS_BY_ID = Arrays.stream(CharClass.values())
            .collect(Collectors.toMap(CharClass::getId, Function.identity()));
    private static final Map<Integer, Gender> GENDER_BY_ID = Arrays.stream(Gender.values())
            .collect(Collectors.toMap(Gender::getId, Function.identity()));

    private final DSLContext dsl;

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
                RACE_BY_ID.get(r.getRace().intValue()),
                CLASS_BY_ID.get(r.getCharClass().intValue()),
                GENDER_BY_ID.get(r.getGender().intValue()),
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