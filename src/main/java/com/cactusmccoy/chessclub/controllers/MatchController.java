package com.cactusmccoy.chessclub.controllers;

import com.cactusmccoy.chessclub.dtos.MatchRequestDto;
import com.cactusmccoy.chessclub.dtos.MatchResponseDto;
import com.cactusmccoy.chessclub.services.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponseDto> createMatch(@Valid @RequestBody MatchRequestDto request) {
        MatchResponseDto response = matchService.registerMatch(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
