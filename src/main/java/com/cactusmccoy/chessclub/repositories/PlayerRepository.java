package com.cactusmccoy.chessclub.repositories;

import com.cactusmccoy.chessclub.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByNickname(String nickname);
}
