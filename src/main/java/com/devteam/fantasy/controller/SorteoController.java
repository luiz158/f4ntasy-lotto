package com.devteam.fantasy.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devteam.fantasy.exception.CanNotChangeWinningNumberException;
import com.devteam.fantasy.exception.CanNotInsertApuestaException;
import com.devteam.fantasy.exception.CanNotInsertHistoricoBalanceException;
import com.devteam.fantasy.exception.CanNotInsertWinningNumberException;
import com.devteam.fantasy.exception.CanNotRemoveApuestaException;
import com.devteam.fantasy.exception.InvalidSorteoStateException;
import com.devteam.fantasy.exception.SorteoEstadoNotValidException;
import com.devteam.fantasy.message.response.ApuestaActivaDetallesResponse;
import com.devteam.fantasy.message.response.ApuestaActivaResponse;
import com.devteam.fantasy.message.response.ApuestaActivaResumenResponse;
import com.devteam.fantasy.message.response.ApuestasActivasResponse;
import com.devteam.fantasy.message.response.JugadorSorteosResponse;
import com.devteam.fantasy.message.response.NumeroPlayerEntryResponse;
import com.devteam.fantasy.message.response.SorteoResponse;
import com.devteam.fantasy.model.SorteoDiaria;
import com.devteam.fantasy.model.User;
import com.devteam.fantasy.service.AdminService;
import com.devteam.fantasy.service.SorteoService;
import com.devteam.fantasy.service.UserService;
import com.devteam.fantasy.util.ProfileIdentifier;
import com.devteam.fantasy.util.Util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javassist.NotFoundException;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/sorteos")
public class SorteoController {

	@Autowired
    private SorteoService sorteoService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    ProfileIdentifier profileIdentifier;
	
	@GetMapping("/activos/{moneda}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public List<ApuestasActivasResponse> getApuestasActivas(@PathVariable String moneda) throws Exception {
    	return sorteoService.getSorteosListWithMoneda(moneda);
    }
	
	@GetMapping("/activosResumen/judadores/{id}/{currency}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public ApuestaActivaResumenResponse getSorteosActivosResumteByIdAndCurrency(@PathVariable Long id, @PathVariable String currency, @Valid @RequestBody ObjectNode json) throws Exception {
    	return sorteoService.getActiveSorteoDetail(id, currency);
	}
	
	@GetMapping("/activos/jugadores/{username}")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS')")
    public List<SorteoResponse> getSorteosActivosByUsername(@PathVariable String username) throws Exception {
        User user = userService.getByUsername(username);
    	List<SorteoDiaria> sorteos = sorteoService.getActiveSorteosList(user);
    	List<SorteoResponse> sorteosResponses = sorteoService.getSorteosResponses(sorteos, user);    	
    	return sorteosResponses;
	}
	
	@GetMapping("/activosResumen/judadores/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public JugadorSorteosResponse findTodaySorteobyUsername(@PathVariable String username) throws Exception {
		User user = userService.getByUsername(username);
		JugadorSorteosResponse jugadorSorteosResponse = new JugadorSorteosResponse();
        jugadorSorteosResponse.setName(user.getName());
        jugadorSorteosResponse.setMoneda(Util.getJugadorFromUser(user).getMoneda().getMonedaName().toString());
        
        List<SorteoDiaria> sorteosDiarios = sorteoService.getActiveSorteosList(user);
        jugadorSorteosResponse.setSorteos(sorteoService.getSorteosResponses(sorteosDiarios, user));
        return jugadorSorteosResponse;
	}
	
	@PutMapping("/bloquear/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> bloquearApuesta(@PathVariable Long id, @Valid @RequestBody ObjectNode json) {
		try {
			if( !profileIdentifier.isProfileUATActive()) {
				sorteoService.forceCloseStatus(id);
		        return ResponseEntity.ok("Sorteo closed");
			}
			sorteoService.bloquearApuesta(id);
		} catch (InvalidSorteoStateException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
        return ResponseEntity.ok("Sorteo locked");
    }
    
    @PutMapping("/desbloquear/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> desbloquearApuesta(@PathVariable Long id, @Valid @RequestBody ObjectNode json) {
    	try {
			sorteoService.desBloquearApuesta(id);
		} catch (InvalidSorteoStateException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
		
        return ResponseEntity.ok("Sorteo unlocked");
    }
    
    @PutMapping("/forceCloseStatus/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> cerrarApuesta(@PathVariable Long id, @Valid @RequestBody ObjectNode json) {
		sorteoService.forceCloseStatus(id);
        return ResponseEntity.ok("Sorteo closed");
    }
    
    @PutMapping("/forceOpenStatus/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> abrirApuesta(@PathVariable Long id, @Valid @RequestBody ObjectNode json) {
		try {
			sorteoService.forceOpenStatus(id);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
        return ResponseEntity.ok("Sorteo closed");
    }
    
    @GetMapping("/activos/detalles/{id}/{moneda}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public ApuestaActivaResumenResponse getDetallesApuestasActivasById(@PathVariable Long id,@PathVariable String moneda) throws Exception {
    	return sorteoService.getDetalleApuestasBySorteo(id, moneda);
    }
    
    @PostMapping("/activos/{id}/apuestas")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS')")
    public ResponseEntity<?> submitApuestas(@Valid @RequestBody ObjectNode json, @PathVariable Long id) {
    	try {
	    	ObjectMapper mapper = new ObjectMapper();
	         String username = mapper.convertValue(json.get("username"), String.class);
	
	         JsonNode listElements = json.get("data");
	         List<NumeroPlayerEntryResponse> data = mapper.convertValue(
	                 listElements, new TypeReference<List<NumeroPlayerEntryResponse>>() {}
	         );
	         
	         List<NumeroPlayerEntryResponse> result = data.stream()
	                 .filter(entry -> 0.0 != entry.getCurrent())
	                 .collect(Collectors.toList());
         
			sorteoService.submitApuestas(username, id, result);

    	} catch (CanNotInsertApuestaException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		} catch (SorteoEstadoNotValidException | NotFoundException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
         
    	return ResponseEntity.ok().body("Update numeros");
    }
    
    @GetMapping("/activos/{id}/apuestas/{username}")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS') or hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public ApuestaActivaResponse getApuestasActivasBySorteoAndJugador(@PathVariable Long id, @PathVariable String username) throws Exception {
        return sorteoService.getApuestasActivasBySorteoAndJugador(id, username);
    }
    
    @GetMapping("/activos/{sorteoDiarioId}/apuestas/detalles/{username}")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS') or hasRole('ADMIN') or hasRole('MASTER') or hasRole('SUPERVISOR')")
    public List<ApuestaActivaDetallesResponse> getApuestasActivasDetallesBySorteoAndJugador(@PathVariable Long sorteoDiarioId, @PathVariable String username) throws NotFoundException {
        return sorteoService.getApuestasActivasDetallesBySorteoAndJugador(sorteoDiarioId, username);
    }
    
    @DeleteMapping("/activos/{id}/apuestas/{numero}")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS')")
    public ResponseEntity<?> deleteApuestasActivasBySorteoAndNumeroAndJugador(@PathVariable Long id, @PathVariable Integer numero) {
    	try {
			sorteoService.deleteAllApuestasOnSorteoDiarioByNumeroAndUser(id, numero);
    	} catch (CanNotRemoveApuestaException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}catch (SorteoEstadoNotValidException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
        return ResponseEntity.ok("Apuesta Eliminada Correctamente.");
    }
    
    @DeleteMapping("/activos/{id}/apuestas")
    @PreAuthorize("hasRole('USER') or hasRole('ASIS')")
    public ResponseEntity<?> deleteApuestasActivasBySorteoAndJugador(@PathVariable Long id) {
    	try {
			sorteoService.deleteAllApuestasOnSorteoDiarioByUser(id);
		} catch (CanNotRemoveApuestaException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}catch (SorteoEstadoNotValidException | NotFoundException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
    	
        return ResponseEntity.ok("Apuesta Eliminada Correctamente.");
    }
    
    @DeleteMapping("/activos/{id}/jugadores/{jugadorId}/apuestas/{numero}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> deleteApuestasActivasBySorteoAndNumeroAndJugador(@PathVariable Long id, @PathVariable Long jugadorId, @PathVariable Integer numero) {
    	try {
    		User user = userService.getById(jugadorId);
			sorteoService.deleteAllApuestasOnSorteoDiarioByNumeroAndUser(id, numero, user);
    	} catch (CanNotRemoveApuestaException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}catch (SorteoEstadoNotValidException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
        return ResponseEntity.ok("Apuesta Eliminada Correctamente.");
    }
    
    @DeleteMapping("/activos/{id}/jugadores/{jugadorId}/apuestas")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> deleteApuestasActivasBySorteoAndJugador(@PathVariable Long id, @PathVariable Long jugadorId) {
    	try {
    		User user = userService.getById(jugadorId);
			sorteoService.deleteAllApuestasOnSorteoDiarioByUser(id,user);
		} catch (CanNotRemoveApuestaException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}catch (SorteoEstadoNotValidException | NotFoundException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
    	
        return ResponseEntity.ok("Apuesta Eliminada Correctamente.");
    }
    
    @DeleteMapping("/activos/{id}/usuarios/{userId}/apuestas/detallesx/{numero}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER') or hasRole('USER') or hasRole('ASIS')")
    public ResponseEntity<?> deleteApuestasActivasDetallesXBySorteoAndNumeroAndJugador(@PathVariable Long id, @PathVariable Long userId, @PathVariable Integer numero) {
    	try {
    		
    		User user = userService.getById(userId);
			sorteoService.deleteAllApuestasDetallesXOnSorteoDiarioByNumeroAndUser(id, numero, user);
			
    	} catch (CanNotRemoveApuestaException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}catch (SorteoEstadoNotValidException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
        return ResponseEntity.ok("Apuesta Eliminada Correctamente.");
    }
    
    @PostMapping("/activos/{id}/numero-ganador")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
    public ResponseEntity<?> setNumeroGanadarByApuestaId( @PathVariable Long id, @Valid @RequestBody ObjectNode jsonNodes) {
    	ObjectMapper mapper = new ObjectMapper();
        Integer numero = mapper.convertValue(jsonNodes.get("numero"), Integer.class);
        try {
			sorteoService.setNumeroGanador(id, numero);
		} catch (CanNotInsertWinningNumberException | NotFoundException cniwne) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(cniwne.getMessage());
		} catch (CanNotInsertHistoricoBalanceException cnihbe) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cnihbe.getMessage());
		}
    	return ResponseEntity.ok("Numero Ganador Asignado correctamente");
    }
    
    
    @PutMapping("/{id}/numero-ganador")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<?> changeWinningNumber(@PathVariable Long id, @Valid @RequestBody  ObjectNode jsonNodes) {
		try {
			ObjectMapper mapper = new ObjectMapper();
	        Integer numero = null;
	        try {
	        	numero = mapper.convertValue(jsonNodes.get("numero"), Integer.class);
	        }catch(Exception numberException) {
	        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(numberException.getMessage());
	        }
			sorteoService.changeWinningNumber(numero,id);
		} catch (NotFoundException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		} catch (CanNotChangeWinningNumberException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
        return ResponseEntity.ok("Numero Ganador Cambiado.");
    }
}










