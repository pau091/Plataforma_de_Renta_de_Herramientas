package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.MantenimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMantenimientoRepository extends JpaRepository<MantenimientoEntity, Long> {
    List<MantenimientoEntity> findByHerramientaId(Long herramientaId);
    List<MantenimientoEntity> findByReservaId(Long reservaId);
}
