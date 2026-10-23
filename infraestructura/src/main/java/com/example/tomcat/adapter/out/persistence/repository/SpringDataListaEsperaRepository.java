package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.ListaEsperaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpringDataListaEsperaRepository extends JpaRepository<ListaEsperaEntity, Long> {
    List<ListaEsperaEntity> findByClienteIdOrderByCreadaEnDesc(Long clienteId);
    /** Orden de llegada: el primero de la lista es el primero de la fila. */
    List<ListaEsperaEntity> findByHerramientaIdAndEstadoInOrderByCreadaEnAscIdAsc(Long herramientaId, Collection<String> estados);
}
