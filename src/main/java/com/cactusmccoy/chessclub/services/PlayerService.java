package com.cactusmccoy.chessclub.services;

import com.cactusmccoy.chessclub.dtos.PlayerRequestDto;
import com.cactusmccoy.chessclub.dtos.PlayerResponseDto;
import com.cactusmccoy.chessclub.models.Player;
import com.cactusmccoy.chessclub.repositories.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // Saves a new player in the database
    @Transactional
    public PlayerResponseDto savePlayer(PlayerRequestDto request) {
        // 1. Convert DTO to Entity
        Player player = new Player();
        player.setFirstName(request.firstName());
        player.setLastName(request.lastName());
        player.setNickname(request.nickname());
        player.setElo(request.elo());

        // 2. Save in Database
        Player savedPlayer = playerRepository.save(player);

        // 3. Convert Entity back to safe Response DTO
        return new PlayerResponseDto(
                savedPlayer.getId(),
                savedPlayer.getFirstName(),
                savedPlayer.getLastName(),
                savedPlayer.getNickname(),
                savedPlayer.getElo()
        );
    }

    // Retrieves the list of all players
    @Transactional(readOnly = true)
    public List<PlayerResponseDto> getAllPlayers() {
        return playerRepository.findAll()
                .stream()
                .map(player -> new PlayerResponseDto(
                        player.getId(),
                        player.getFirstName(),
                        player.getLastName(),
                        player.getNickname(),
                        player.getElo()
                ))
                .collect(Collectors.toList());
    }
}