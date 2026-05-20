package com.example.world.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.upper;
import static org.jooq.impl.DSL.val;

@Repository
@RequiredArgsConstructor
public class WorldAccountRepository {

    private final DSLContext dsl;

    public byte[] findSessionKeyByUsername(String username) {
        var sessionKey = field("session_key", byte[].class);
        return dsl.select(sessionKey)
                .from(table("auth.account"))
                .where(upper(field("username", String.class)).eq(upper(val(username))))
                .fetchOne(sessionKey);
    }
}
