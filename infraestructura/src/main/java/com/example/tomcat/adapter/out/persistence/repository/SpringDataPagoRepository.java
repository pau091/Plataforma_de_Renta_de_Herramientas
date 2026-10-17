package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataPagoRepository extends JpaRepository<PagoEntity, Long> {
    List<PagoEntity> findByReservaId(Long reservaId);
    boolean existsByReservaIdAndEstadoPago(Long reservaId, String estadoPago);
}
