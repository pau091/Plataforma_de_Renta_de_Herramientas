package com.example.tomcat.repository;

import com.example.tomcat.model.Herramienta;

import java.util.List;
import java.util.Optional;

public interface HerramientaRepositoryPort {
    Herramienta guardar(Herramienta herramienta);
    List<Herramienta> listar();
    List<Herramienta> listarDisponibles();
    List<Herramienta> listarPorProveedor(Long proveedorId);
    Optional<Herramienta> buscarPorId(Long id);
    void eliminar(Long id);
}
