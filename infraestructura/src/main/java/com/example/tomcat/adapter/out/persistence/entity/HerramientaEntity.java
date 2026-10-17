package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "herramientas")
public class HerramientaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "tarifa_diaria", nullable = false)
    private BigDecimal tarifaDiaria;

    @Column(nullable = false)
    private String estado;

    @Column(name = "imagen_url")
    private String imagenUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getTarifaDiaria() { return tarifaDiaria; }
    public void setTarifaDiaria(BigDecimal tarifaDiaria) { this.tarifaDiaria = tarifaDiaria; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
