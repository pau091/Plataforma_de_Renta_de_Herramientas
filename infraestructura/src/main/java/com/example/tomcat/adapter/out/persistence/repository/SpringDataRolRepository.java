package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataRolRepository extends JpaRepository<RolEntity, Long> {
    Optional<RolEntity> findByNombre(String nombre);
}
