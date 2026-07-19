package com.cactusmccoy.chessclub.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlayerRequestDto(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Nickname is required")
        String nickname,

        @Min(value = 0, message = "ELO cannot be negative")
        Integer elo
) {}
