package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.TokenEmitidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataTokenEmitidoRepository extends JpaRepository<TokenEmitidoEntity, String> {

    List<TokenEmitidoEntity> findByUsuarioIdAndExpiraEnAfter(Long usuarioId, LocalDateTime ahora);

    @Modifying
    @Query("DELETE FROM TokenEmitidoEntity t WHERE t.expiraEn < :ahora")
    int borrarExpirados(@Param("ahora") LocalDateTime ahora);
}
