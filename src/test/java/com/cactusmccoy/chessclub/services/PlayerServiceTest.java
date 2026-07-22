package com.cactusmccoy.chessclub.services;

import com.cactusmccoy.chessclub.dtos.PlayerRequestDto;
import com.cactusmccoy.chessclub.dtos.PlayerResponseDto;
import com.cactusmccoy.chessclub.models.Player;
import com.cactusmccoy.chessclub.repositories.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void savePlayer_persistsMappedEntityAndReturnsResponse() {
        PlayerRequestDto request = new PlayerRequestDto("Ada", "Lovelace", "analytical", 1800);

        when(playerRepository.existsByNickname("analytical")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            player.setId(1L);
            return player;
        });

        PlayerResponseDto response = playerService.savePlayer(request);

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(playerCaptor.capture());
        Player saved = playerCaptor.getValue();

        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getNickname()).isEqualTo("analytical");
        assertThat(saved.getElo()).isEqualTo(1800);

        assertThat(response).isEqualTo(
                new PlayerResponseDto(1L, "Ada", "Lovelace", "analytical", 1800)
        );
    }

    @Test
    void savePlayer_whenNicknameExists_throwsConflict() {
        PlayerRequestDto request = new PlayerRequestDto("Ada", "Lovelace", "analytical", 1800);
        when(playerRepository.existsByNickname("analytical")).thenReturn(true);

        assertThatThrownBy(() -> playerService.savePlayer(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusException = (ResponseStatusException) ex;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusException.getReason()).isEqualTo("Nickname is already taken");
                });

        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    void getAllPlayers_mapsEntitiesToResponseDtos() {
        Player player = new Player();
        player.setId(7L);
        player.setFirstName("Magnus");
        player.setLastName("Carlsen");
        player.setNickname("magnus");
        player.setElo(2830);

        when(playerRepository.findAll()).thenReturn(List.of(player));

        List<PlayerResponseDto> response = playerService.getAllPlayers();

        assertThat(response).containsExactly(
                new PlayerResponseDto(7L, "Magnus", "Carlsen", "magnus", 2830)
        );
    }
}
