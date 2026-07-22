package com.cactusmccoy.chessclub.repositories;

import com.cactusmccoy.chessclub.models.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    // Solo la definición de la interfaz
}