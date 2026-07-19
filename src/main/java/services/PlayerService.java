package com.cactusmccoy.chessclub.services;

import com.cactusmccoy.chessclub.models.Player;
import com.cactusmccoy.chessclub.repositories.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    // Inyectamos el repositorio para poder usar sus comandos
    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // Método para guardar un nuevo participante en la base de datos
    public Player savePlayer(Player player) {
        // TODO: Más adelante agregaremos validaciones aquí (ej. que el alias no exista ya)
        return playerRepository.save(player);
    }

    // Método para obtener la lista de todos los participantes
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }
}