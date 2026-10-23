package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.CalificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataCalificacionRepository extends JpaRepository<CalificacionEntity, Long> {
    boolean existsByReservaId(Long reservaId);
    List<CalificacionEntity> findByHerramientaIdOrderByFechaDesc(Long herramientaId);
    List<CalificacionEntity> findByClienteIdOrderByFechaDesc(Long clienteId);
}
