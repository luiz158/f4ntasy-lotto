package com.devteam.fantasy.repository;

import com.devteam.fantasy.model.Estado;
import com.devteam.fantasy.model.Sorteo;
import com.devteam.fantasy.model.SorteoDiaria;
import com.devteam.fantasy.model.SorteoType;
import com.devteam.fantasy.model.Status;
import com.devteam.fantasy.util.EstadoName;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public interface SorteoRepository extends CrudRepository<Sorteo, Long> {
    Sorteo getSorteoBySorteoTime(Timestamp timestamp);

    Sorteo getSorteoById(Long id);

    boolean existsSorteoBySorteoTime(Timestamp timestamp);

    List<Sorteo> findAllByStatus(Status status);

    @Query(value = "from Sorteo t where t.sorteoTime BETWEEN :startDate AND :endDate")
    List<Sorteo> getAllBetweenTimestamp(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    List<Sorteo> findTop60ByEstadoOrderByIdDesc(Estado estado);

    Sorteo getSorteoBySorteoTypeEquals(SorteoType sorteoType);

	Iterable<Sorteo> findAllByOrderByIdAsc();

	List<Sorteo> findAllBySorteoTimeBetween(Timestamp start, Timestamp end);

	List<Sorteo> findAllBySorteoTimeBetweenOrderBySorteoTime(Timestamp monday, Timestamp sunday);

	@Query("SELECt s From NumeroGanador ng JOIN ng.sorteo s WHERE s.sorteoTime BETWEEN :monday AND :sunday AND ng.sorteo = s ORDER BY s.sorteoTime")
	List<Sorteo> findAllBySorteoTimeBetweenOrderBySorteoTimeWithNumeroGanadorNotNull(Timestamp monday, Timestamp sunday);

	List<Sorteo> findAllBySorteoTimeBetweenAndEstadoOrderBySorteoTime(Timestamp monday, Timestamp sunday,
			Estado estadoByEstado);
	
}
