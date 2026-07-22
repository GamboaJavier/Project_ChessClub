package com.cactusmccoy.chessclub.dtos;

import jakarta.validation.constraints.NotNull;

public record MatchRequestDto(
        @NotNull(message = "White player ID is required")
        Long whitePlayerId,

        @NotNull(message = "Black player ID is required")
        Long blackPlayerId,

        @NotNull(message = "Result is required")
        String result
) {}
