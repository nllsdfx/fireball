package com.example.world.domain.port.out;

import com.example.world.domain.model.Character;
import lombok.NonNull;

import java.util.List;

public interface CharacterRepository {
    @NonNull List<Character> findByAccountId(@NonNull Long accountId);
}