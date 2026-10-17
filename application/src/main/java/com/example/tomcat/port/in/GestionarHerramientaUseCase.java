package com.example.tomcat.port.in;

import com.example.tomcat.model.Herramienta;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarHerramientaUseCase {
    Herramienta crear(Long proveedorId, Long categoriaId, String nombre, String descripcion, BigDecimal tarifaDiaria, String imagenUrl);
    Herramienta actualizar(Long id, Long categoriaId, String nombre, String descripcion, BigDecimal tarifaDiaria, String estado, String imagenUrl);
    void eliminar(Long id);
    Herramienta cambiarEstado(Long id, String estado);
    Herramienta actualizarImagen(Long id, String imagenUrl);
    List<Herramienta> listar();
    List<Herramienta> listarDisponibles();
    List<Herramienta> listarPorProveedor(Long proveedorId);
    Herramienta buscarPorId(Long id);
}
