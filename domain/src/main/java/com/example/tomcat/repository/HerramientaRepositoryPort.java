package com.example.tomcat.repository;

import com.example.tomcat.model.Herramienta;

import java.util.List;
import java.util.Optional;

public interface HerramientaRepositoryPort {
    Herramienta guardar(Herramienta herramienta);
    List<Herramienta> listar();
    List<Herramienta> listarDisponibles();
    List<Herramienta> listarPorProveedor(Long proveedorId);
    /** Búsqueda del catálogo: cada filtro es opcional (null = sin filtrar); texto busca en nombre y descripción. */
    List<Herramienta> buscar(Long categoriaId, Long proveedorId, String estado, String texto);
    Optional<Herramienta> buscarPorId(Long id);
    /**
     * Como buscarPorId, pero bloquea la herramienta hasta que termine la transacción. Quien reserva fechas de una
     * herramienta pasa por aquí, así que dos reservas simultáneas del mismo equipo se atienden de una en una.
     */
    Optional<Herramienta> bloquearPorId(Long id);
    boolean existePorCategoria(Long categoriaId);
    void eliminar(Long id);
}
