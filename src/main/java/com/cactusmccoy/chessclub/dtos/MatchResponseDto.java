package com.cactusmccoy.chessclub.dtos;

import java.time.LocalDateTime;

public record MatchResponseDto(
        Long id,
        String whitePlayerName,
        String blackPlayerName,
        String result,
        LocalDateTime matchDate
) {}