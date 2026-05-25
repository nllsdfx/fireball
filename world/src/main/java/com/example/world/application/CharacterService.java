package com.example.world.application;

import com.example.world.domain.model.Character;
import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.domain.port.out.CharacterRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CharacterService implements CharacterUseCase {

    private final CharacterRepository characterRepository;

    @Override
    public @NonNull List<Character> listCharacters(@NonNull Long accountId) {
        return characterRepository.findByAccountId(accountId);
    }

    @Override
    public @NonNull Optional<Character> getCharacter(@NonNull Long id, @NonNull Long accountId) {
        return characterRepository.findById(id, accountId);
    }

    @Override
    public void createCharacter(@NonNull Character character) {
        characterRepository.create(character);
    }

    @Override
    public boolean deleteCharacter(@NonNull Long characterId, @NonNull Long accountId) {
        return characterRepository.delete(characterId, accountId);
    }
}