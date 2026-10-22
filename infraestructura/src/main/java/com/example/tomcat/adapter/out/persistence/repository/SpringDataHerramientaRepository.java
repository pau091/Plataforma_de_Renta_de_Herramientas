package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.HerramientaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataHerramientaRepository extends JpaRepository<HerramientaEntity, Long> {
    List<HerramientaEntity> findByEstado(String estado);
    List<HerramientaEntity> findByProveedorId(Long proveedorId);
    boolean existsByCategoriaId(Long categoriaId);

    /** SELECT ... FOR UPDATE: la fila queda bloqueada hasta el commit o rollback de la transacción. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HerramientaEntity h WHERE h.id = :id")
    Optional<HerramientaEntity> findByIdParaActualizar(@Param("id") Long id);

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
