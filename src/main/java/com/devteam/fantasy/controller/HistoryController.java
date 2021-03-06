package com.devteam.fantasy.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devteam.fantasy.message.response.ApuestaActivaDetallesResponse;
import com.devteam.fantasy.message.response.ApuestaActivaResumenResponse;
import com.devteam.fantasy.message.response.HistoricoApuestaDetallesResponse;
import com.devteam.fantasy.message.response.NumeroGanadorSorteoResponse;
import com.devteam.fantasy.message.response.SorteosPasadosApuestas;
import com.devteam.fantasy.message.response.SorteosPasados;
import com.devteam.fantasy.message.response.WeekResponse;
import com.devteam.fantasy.model.Jugador;
import com.devteam.fantasy.model.User;
import com.devteam.fantasy.service.HistoryService;
import com.devteam.fantasy.service.UserService;
import com.devteam.fantasy.util.Util;
import javassist.NotFoundException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/history")
public class HistoryController {

	@Autowired
	HistoryService historyService;
	
	@Autowired
	UserService userService;
	
	@GetMapping("/numeros/ganadores/{currency}")
	public ResponseEntity<List<NumeroGanadorSorteoResponse>> numerosGanadores(@PathVariable String currency){
		List<NumeroGanadorSorteoResponse> result = null;
		try {
			result = historyService.getNumerosGanadores(currency);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);			
		}
		return new ResponseEntity<List<NumeroGanadorSorteoResponse>>(result,HttpStatus.OK);
	}
	
	@GetMapping("/weeks")
	public List<WeekResponse> getWeeksList() {
		return historyService.getAllWeeks();
	}
	
	@GetMapping("/weeks/{id}/{historyType}/{moneda}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR') ")
	public ResponseEntity<Object> getWeekOverview(@PathVariable Long id, @PathVariable String historyType, @PathVariable String moneda) {
		Object result = null;
		try {
			if("casa".equalsIgnoreCase(historyType)) {
				result = historyService.getSorteosPasadosCasaByWeek(id, moneda);
			}else if("vendedor".equalsIgnoreCase(historyType)) {
				result = historyService.getSorteosPasadosJugadoresByWeek(id, moneda);
			}
			
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Object>(result,HttpStatus.OK);
	}
	
	@GetMapping("/weeks/{id}/jugador")
	public ResponseEntity<SorteosPasados> getWeekOverviewByJugador(@PathVariable Long id){
		SorteosPasados result = null;
		try {
			User user = userService.getLoggedInUser();
			result = historyService.getSorteosPasadosJugadorByWeek(id, user);
		} catch (Exception e) {
			return new ResponseEntity<SorteosPasados>(result,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SorteosPasados>(result,HttpStatus.OK);
	}
	
	@GetMapping("/weeks/{weekId}/jugador/{jugadorId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
	public ResponseEntity<SorteosPasados> getWeekOverviewByJugador(@PathVariable Long weekId, @PathVariable Long jugadorId){
		SorteosPasados result = null;
		try {
			User user = userService.getById(jugadorId);
			Jugador jugador = Util.getJugadorFromUser(user);
			
			if(jugador != null) {
				result = historyService.getSorteosPasadosJugadorByWeek(weekId, jugador);
			}else {
				throw new NotFoundException("Jugador with id "+jugadorId+" not found");
			}
		} catch (Exception e) {
			return new ResponseEntity<SorteosPasados>(result,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SorteosPasados>(result,HttpStatus.OK);
	}
	
	
	@GetMapping("/weeks/jugador/sorteo/{id}")
	public ResponseEntity<SorteosPasadosApuestas> getApuestasOverviewBySorteo(@PathVariable Long id){
		SorteosPasadosApuestas result = null;
		try {
			User user = userService.getLoggedInUser();
			result = historyService.getApuestasPasadasBySorteoAndJugador(id,user);
		} catch (Exception e) {
			return new ResponseEntity<SorteosPasadosApuestas>(result,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SorteosPasadosApuestas>(result,HttpStatus.OK);
	}
	
	@GetMapping("/weeks/jugador/sorteo/{id}/{currency}")
	public ResponseEntity<SorteosPasadosApuestas> getApuestasOverviewBySorteo(@PathVariable Long id, @PathVariable String currency){
		SorteosPasadosApuestas result = null;
		try {
			User user = userService.getLoggedInUser();
			result = historyService.getApuestasPasadasBySorteoAndJugador(id, currency, user);
		} catch (Exception e) {
			return new ResponseEntity<SorteosPasadosApuestas>(result,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SorteosPasadosApuestas>(result,HttpStatus.OK);
	}
	
	@GetMapping("/weeks/jugador/{jugadorId}/sorteo/{id}")
	public ResponseEntity<SorteosPasadosApuestas> getApuestasOverviewBySorteoAndJugador(@PathVariable Long jugadorId, @PathVariable Long id){
		SorteosPasadosApuestas result = null;
		try {
			User user = userService.getById(jugadorId);
			Jugador jugador = Util.getJugadorFromUser(user);
			if(jugador != null) {
				result = historyService.getApuestasPasadasBySorteoAndJugador(id, jugador);
			}
		} catch (Exception e) {
			return new ResponseEntity<SorteosPasadosApuestas>(result,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SorteosPasadosApuestas>(result,HttpStatus.OK);
	}
	
	
	@GetMapping("/sorteos/{id}/apuestas/detalles")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS')")
    public ResponseEntity<List<HistoricoApuestaDetallesResponse>> getHistoricoApuestaDetallesX(@PathVariable Long id) {
		List<HistoricoApuestaDetallesResponse> result = null;
		try {
        	result = historyService.getHistoricoApuestaDetallesX(id);
        }catch (Exception e) {
        	return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<List<HistoricoApuestaDetallesResponse>>(result,HttpStatus.OK);
    }
	
	@GetMapping("/sorteos/{id}/apuestas/detalles/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS') or hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public List<ApuestaActivaDetallesResponse> getApuestasBySorteoAndJugador(@PathVariable Long id, @PathVariable Long userId) {
		
        return historyService.getHistoricoApuestasBySorteoAndJugador(id, userId);
    }
	
	@GetMapping("/sorteos/{id}/apuestas/riesgo/{moneda}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public ApuestaActivaResumenResponse getDetallesRiesgoBySorteoId(@PathVariable Long id,@PathVariable String moneda) {
    	return historyService.getRiesgoHistoricoBySorteo(id, moneda);
    }
}
