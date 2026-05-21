package com.example.world.persistence.repository;

import com.example.world.service.AccountSession;
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

    public AccountSession findAccountSession(String username) {
        return dsl.select(field("id", Long.class), field("session_key", byte[].class))
                .from(table("auth.account"))
                .where(upper(field("username", String.class)).eq(upper(val(username))))
                .fetchOne(r -> new AccountSession(
                        r.get(field("id", Long.class)),
                        r.get(field("session_key", byte[].class))
                ));
    }
}