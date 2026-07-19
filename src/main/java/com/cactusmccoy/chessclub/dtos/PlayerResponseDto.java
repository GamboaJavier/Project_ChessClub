package com.cactusmccoy.chessclub.dtos;

public record PlayerResponseDto(
        Long id,
        String firstName,
        String lastName,
        String nickname,
        Integer elo
) {}
