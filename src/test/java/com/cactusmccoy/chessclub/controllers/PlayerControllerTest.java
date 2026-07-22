package com.cactusmccoy.chessclub.controllers;

import com.cactusmccoy.chessclub.dtos.PlayerRequestDto;
import com.cactusmccoy.chessclub.dtos.PlayerResponseDto;
import com.cactusmccoy.chessclub.exceptions.GlobalExceptionHandler;
import com.cactusmccoy.chessclub.services.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @Test
    void createPlayer_withValidPayload_returnsCreated() throws Exception {
        PlayerResponseDto response = new PlayerResponseDto(1L, "Ada", "Lovelace", "analytical", 1800);
        when(playerService.savePlayer(any(PlayerRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada",
                                  "lastName": "Lovelace",
                                  "nickname": "analytical",
                                  "elo": 1800
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("analytical"));
    }

    @Test
    void createPlayer_withBlankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "Lovelace",
                                  "nickname": "analytical",
                                  "elo": 1800
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"));

        verify(playerService, never()).savePlayer(any(PlayerRequestDto.class));
    }

    @Test
    void createPlayer_withNegativeElo_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada",
                                  "lastName": "Lovelace",
                                  "nickname": "analytical",
                                  "elo": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("elo"));

        verify(playerService, never()).savePlayer(any(PlayerRequestDto.class));
    }

    @Test
    void getAllPlayers_returnsOk() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(List.of(
                new PlayerResponseDto(1L, "Ada", "Lovelace", "analytical", 1800)
        ));

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nickname").value("analytical"));
    }
}
