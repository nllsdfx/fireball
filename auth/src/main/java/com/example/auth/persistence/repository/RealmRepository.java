package com.example.auth.persistence.repository;

import com.example.auth.persistence.generated.tables.records.RealmRecord;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.auth.persistence.generated.Tables.REALM;

@Repository
@RequiredArgsConstructor
public class RealmRepository {
    private final DSLContext dslContext;

    @NonNull
    public List<RealmRecord> findAll() {
        return dslContext.selectFrom(REALM).fetch();
    }
}
