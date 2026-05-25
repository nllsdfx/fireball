package com.example.world.domain.port.in;

import com.example.world.domain.model.Character;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public interface CharacterUseCase {
    @NonNull List<Character> listCharacters(@NonNull Long accountId);
    @NonNull Optional<Character> getCharacter(@NonNull Long id, @NonNull Long accountId);
    void createCharacter(@NonNull Character character);
    boolean deleteCharacter(@NonNull Long characterId, @NonNull Long accountId);
}