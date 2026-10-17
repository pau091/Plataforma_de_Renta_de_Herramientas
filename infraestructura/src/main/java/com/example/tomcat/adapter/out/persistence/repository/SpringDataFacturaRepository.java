package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.FacturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataFacturaRepository extends JpaRepository<FacturaEntity, Long> {
    Optional<FacturaEntity> findByPagoId(Long pagoId);
    Optional<FacturaEntity> findByReservaId(Long reservaId);
    List<FacturaEntity> findByClienteId(Long clienteId);
    List<FacturaEntity> findByHerramientaIdIn(Collection<Long> herramientaIds);
}
