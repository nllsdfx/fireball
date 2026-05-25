package com.example.world.domain.port.out;

import com.example.world.domain.model.Character;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository {
    @NonNull List<Character> findByAccountId(@NonNull Long accountId);
    @NonNull Optional<Character> findById(@NonNull Long id, @NonNull Long accountId);
    void create(@NonNull Character character);
    boolean delete(@NonNull Long characterId, @NonNull Long accountId);
}