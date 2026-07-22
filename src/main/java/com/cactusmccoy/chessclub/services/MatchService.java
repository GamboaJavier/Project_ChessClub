package com.cactusmccoy.chessclub.services;

import com.cactusmccoy.chessclub.dtos.MatchRequestDto;
import com.cactusmccoy.chessclub.dtos.MatchResponseDto;
import com.cactusmccoy.chessclub.models.Match;
import com.cactusmccoy.chessclub.models.Player;
import com.cactusmccoy.chessclub.repositories.MatchRepository;
import com.cactusmccoy.chessclub.repositories.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    public MatchService(MatchRepository matchRepository, PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public MatchResponseDto registerMatch(MatchRequestDto request) {
        if (request.whitePlayerId().equals(request.blackPlayerId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "White player and Black player cannot be the same person"
            );
        }

        Player white = playerRepository.findById(request.whitePlayerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "White player with ID " + request.whitePlayerId() + " not found"
                ));

        Player black = playerRepository.findById(request.blackPlayerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Black player with ID " + request.blackPlayerId() + " not found"
                ));

        Match match = new Match();
        match.setWhitePlayer(white);
        match.setBlackPlayer(black);
        match.setResult(request.result());

        Match savedMatch = matchRepository.save(match);

        return new MatchResponseDto(
                savedMatch.getId(),
                white.getName(),
                black.getName(),
                savedMatch.getResult(),
                savedMatch.getMatchDate()
        );
    }
}