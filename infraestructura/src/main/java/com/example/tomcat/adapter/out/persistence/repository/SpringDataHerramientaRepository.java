package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.HerramientaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataHerramientaRepository extends JpaRepository<HerramientaEntity, Long> {
    List<HerramientaEntity> findByEstado(String estado);
    List<HerramientaEntity> findByProveedorId(Long proveedorId);
}
