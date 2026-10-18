package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.ReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SpringDataReservaRepository extends JpaRepository<ReservaEntity, Long> {
    List<ReservaEntity> findByClienteId(Long clienteId);
    List<ReservaEntity> findByHerramientaId(Long herramientaId);
    List<ReservaEntity> findByEstadoIn(Collection<String> estados);
    List<ReservaEntity> findByEstadoDevolucionIn(Collection<String> estadosDevolucion);

    @Query("SELECT r FROM ReservaEntity r WHERE r.herramientaId IN :herramientaIds")
    List<ReservaEntity> findByHerramientaIdIn(@Param("herramientaIds") List<Long> herramientaIds);

    @Query("""
            SELECT COUNT(r) > 0 FROM ReservaEntity r
            WHERE r.herramientaId = :herramientaId
              AND r.estado IN ('PENDIENTE', 'ACEPTADA', 'CONFIRMADA', 'ENTREGADA')
              AND r.fechaInicio <= :fechaFin
              AND r.fechaFin >= :fechaInicio
              AND (:reservaIdExcluida IS NULL OR r.id <> :reservaIdExcluida)
            """)
    boolean existsSolape(
            @Param("herramientaId") Long herramientaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("reservaIdExcluida") Long reservaIdExcluida
    );
}
