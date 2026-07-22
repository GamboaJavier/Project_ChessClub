package com.cactusmccoy.chessclub.controllers;

import com.cactusmccoy.chessclub.dtos.PlayerRequestDto;
import com.cactusmccoy.chessclub.dtos.PlayerResponseDto;
import com.cactusmccoy.chessclub.services.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // Endpoint to create a new player
    @PostMapping
    public ResponseEntity<PlayerResponseDto> createPlayer(@Valid @RequestBody PlayerRequestDto request) {
        PlayerResponseDto createdPlayer = playerService.savePlayer(request);
        return new ResponseEntity<>(createdPlayer, HttpStatus.CREATED);
    }

    // Endpoint to get all players
    @GetMapping
    public ResponseEntity<List<PlayerResponseDto>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }
}
