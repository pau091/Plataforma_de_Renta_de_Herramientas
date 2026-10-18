package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.HerramientaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataHerramientaRepository extends JpaRepository<HerramientaEntity, Long> {
    List<HerramientaEntity> findByEstado(String estado);
    List<HerramientaEntity> findByProveedorId(Long proveedorId);
    boolean existsByCategoriaId(Long categoriaId);

    @Query("""
            SELECT h FROM HerramientaEntity h
            WHERE (:categoriaId IS NULL OR h.categoriaId = :categoriaId)
              AND (:proveedorId IS NULL OR h.proveedorId = :proveedorId)
              AND (:estado IS NULL OR h.estado = :estado)
              AND (:texto IS NULL
                   OR LOWER(h.nombre) LIKE CONCAT('%', :texto, '%')
                   OR LOWER(h.descripcion) LIKE CONCAT('%', :texto, '%'))
            ORDER BY h.id
            """)
    List<HerramientaEntity> buscar(@Param("categoriaId") Long categoriaId,
                                   @Param("proveedorId") Long proveedorId,
                                   @Param("estado") String estado,
                                   @Param("texto") String texto);
}
