package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.TokenRevocadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SpringDataTokenRevocadoRepository extends JpaRepository<TokenRevocadoEntity, String> {

    long countByUsuarioId(Long usuarioId);

    @Modifying
    @Query("DELETE FROM TokenRevocadoEntity t WHERE t.expiraEn < :ahora")
    int borrarExpirados(@Param("ahora") LocalDateTime ahora);
}
