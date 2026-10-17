package com.example.tomcat.port.in;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.port.in.dto.Solicitante;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarHerramientaUseCase {
    /**
     * Si el solicitante es PROVEEDOR la herramienta se registra a su nombre; si es ADMIN
     * debe indicar el proveedorId del dueño.
     */
    Herramienta crear(Solicitante solicitante, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                      BigDecimal tarifaDiaria, String imagenUrl);
    Herramienta actualizar(Solicitante solicitante, Long id, Long categoriaId, String nombre, String descripcion,
                           BigDecimal tarifaDiaria, String estado, String imagenUrl);
    void eliminar(Solicitante solicitante, Long id);
    Herramienta cambiarEstado(Solicitante solicitante, Long id, String estado);
    Herramienta actualizarImagen(Solicitante solicitante, Long id, String imagenUrl);
    /** Sube un archivo de imagen (JPG, PNG, WEBP o GIF, máx. 5 MB) y lo asigna a la herramienta. */
    Herramienta subirImagen(Solicitante solicitante, Long id, byte[] contenido);

    // Consultas del catálogo: abiertas a cualquier usuario autenticado.
    List<Herramienta> listar();
    List<Herramienta> listarDisponibles();
    List<Herramienta> listarPorProveedor(Long proveedorId);
    Herramienta buscarPorId(Long id);
}
