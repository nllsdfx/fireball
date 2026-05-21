package com.example.world.application;

import com.example.world.domain.model.Character;
import com.example.world.domain.port.in.CharacterUseCase;
import com.example.world.domain.port.out.CharacterRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterService implements CharacterUseCase {

    private final CharacterRepository characterRepository;

    @Override
    public @NonNull List<Character> listCharacters(@NonNull Long accountId) {
        return characterRepository.findByAccountId(accountId);
    }
}