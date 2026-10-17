package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.math.BigDecimal;
import java.util.Set;

public class Herramienta {
    private static final Set<String> ESTADOS = Set.of("DISPONIBLE", "MANTENIMIENTO", "FUERA_DE_SERVICIO");

    private Long id;
    private Long proveedorId;
    private Long categoriaId;
    private String nombre;
    private String descripcion;
    private BigDecimal tarifaDiaria;
    private String estado;
    private String imagenUrl;

    public Herramienta(Long id, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                       BigDecimal tarifaDiaria, String estado, String imagenUrl) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tarifaDiaria = tarifaDiaria;
        this.estado = estado != null ? estado : "DISPONIBLE";
        this.imagenUrl = imagenUrl;
    }

    public Long getId() { return id; }
    public Long getProveedorId() { return proveedorId; }
    public Long getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getTarifaDiaria() { return tarifaDiaria; }
    public String getEstado() { return estado; }
    public String getImagenUrl() { return imagenUrl; }

    public boolean estaDisponible() {
        return "DISPONIBLE".equals(estado);
    }

    public void actualizar(Long categoriaId, String nombre, String descripcion, BigDecimal tarifaDiaria, String estado, String imagenUrl) {
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tarifaDiaria = tarifaDiaria;
        if (estado != null) {
            cambiarEstado(estado);
        }
        if (imagenUrl != null) {
            this.imagenUrl = imagenUrl;
        }
    }

    public void cambiarEstado(String nuevoEstado) {
        if (nuevoEstado == null || !ESTADOS.contains(nuevoEstado)) {
            throw new DomainException("Estado de herramienta no válido. Use DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO.");
        }
        this.estado = nuevoEstado;
    }

    public void actualizarImagen(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
