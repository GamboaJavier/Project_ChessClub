package com.cactusmccoy.chessclub.controllers;

import com.cactusmccoy.chessclub.models.Player;
import com.cactusmccoy.chessclub.services.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // Endpoint para que el frontend envíe un nuevo jugador y se guarde
    @PostMapping
    public Player createPlayer(@RequestBody Player player) {
        return playerService.savePlayer(player);
    }

    // Endpoint para que el frontend pida la lista de todos los jugadores
    @GetMapping
    public List<Player> getAllPlayers() {
        return playerService.getAllPlayers();
    }
}