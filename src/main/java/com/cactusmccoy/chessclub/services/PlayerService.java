package com.cactusmccoy.chessclub.services;

import com.cactusmccoy.chessclub.dtos.PlayerRequestDto;
import com.cactusmccoy.chessclub.dtos.PlayerResponseDto;
import com.cactusmccoy.chessclub.models.Player;
import com.cactusmccoy.chessclub.repositories.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public PlayerResponseDto savePlayer(PlayerRequestDto request) {
        if (playerRepository.existsByNickname(request.nickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nickname is already taken");
        }

        Player player = new Player();
        player.setFirstName(request.firstName());
        player.setLastName(request.lastName());
        player.setNickname(request.nickname());
        player.setElo(request.elo());

        Player savedPlayer = playerRepository.save(player);
        return toResponseDto(savedPlayer);
    }

    public List<PlayerResponseDto> getAllPlayers() {
        return playerRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    private PlayerResponseDto toResponseDto(Player player) {
        return new PlayerResponseDto(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getNickname(),
                player.getElo()
        );
    }
}
