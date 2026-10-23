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
    /** Depósito de garantía que fija el proveedor; se cobra con el alquiler y se liquida al devolver (0 = sin depósito). */
    private BigDecimal deposito;

    public Herramienta(Long id, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                       BigDecimal tarifaDiaria, String estado, String imagenUrl) {
        this(id, proveedorId, categoriaId, nombre, descripcion, tarifaDiaria, estado, imagenUrl, BigDecimal.ZERO);
    }

    public Herramienta(Long id, Long proveedorId, Long categoriaId, String nombre, String descripcion,
                       BigDecimal tarifaDiaria, String estado, String imagenUrl, BigDecimal deposito) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tarifaDiaria = tarifaDiaria;
        this.estado = estado != null ? estado : "DISPONIBLE";
        this.imagenUrl = imagenUrl;
        this.deposito = deposito != null ? deposito : BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public Long getProveedorId() { return proveedorId; }
    public Long getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getTarifaDiaria() { return tarifaDiaria; }
    public String getEstado() { return estado; }
    public String getImagenUrl() { return imagenUrl; }
    public BigDecimal getDeposito() { return deposito; }

    public boolean estaDisponible() {
        return "DISPONIBLE".equals(estado);
    }

    public void actualizar(Long categoriaId, String nombre, String descripcion, BigDecimal tarifaDiaria, String estado, String imagenUrl) {
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        if (descripcion != null) {
            this.descripcion = descripcion;
        }
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

    public void cambiarDeposito(BigDecimal nuevoDeposito) {
        if (nuevoDeposito == null || nuevoDeposito.signum() < 0) {
            throw new DomainException("El depósito de garantía no puede ser negativo (use 0 si no exige depósito).");
        }
        this.deposito = nuevoDeposito;
    }

    public void actualizarImagen(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
