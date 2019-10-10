package com.devteam.fantasy.repository;

import com.devteam.fantasy.model.Asistente;
import com.devteam.fantasy.model.Jugador;
import com.devteam.fantasy.model.UserState;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsistenteRepository extends JpaRepository<Asistente, Long> {
    List<Asistente> findAllByJugadorAndUserState(Jugador jugador, UserState userState);


}
