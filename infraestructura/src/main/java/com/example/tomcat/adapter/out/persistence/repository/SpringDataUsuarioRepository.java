package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM UsuarioEntity u JOIN u.roles r WHERE r.nombre = :rol")
    List<UsuarioEntity> findByRolNombre(@Param("rol") String rol);
}