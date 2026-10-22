package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.ReservaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataReservaRepository extends JpaRepository<ReservaEntity, Long> {
    List<ReservaEntity> findByClienteId(Long clienteId);
    List<ReservaEntity> findByHerramientaId(Long herramientaId);
    List<ReservaEntity> findByEstadoIn(Collection<String> estados);
    List<ReservaEntity> findByEstadoDevolucionIn(Collection<String> estadosDevolucion);

    /** SELECT ... FOR UPDATE: la fila queda bloqueada hasta el commit o rollback de la transacción. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaEntity r WHERE r.id = :id")
    Optional<ReservaEntity> findByIdParaActualizar(@Param("id") Long id);

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
