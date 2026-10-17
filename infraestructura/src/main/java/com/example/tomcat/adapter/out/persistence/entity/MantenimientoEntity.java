package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mantenimientos")
public class MantenimientoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "herramienta_id", nullable = false)
    private Long herramientaId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "descripcion_problema", nullable = false)
    private String descripcionProblema;

    @Column(name = "costo_reparacion")
    private BigDecimal costoReparacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHerramientaId() { return herramientaId; }
    public void setHerramientaId(Long herramientaId) { this.herramientaId = herramientaId; }
    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }
    public String getDescripcionProblema() { return descripcionProblema; }
    public void setDescripcionProblema(String descripcionProblema) { this.descripcionProblema = descripcionProblema; }
    public BigDecimal getCostoReparacion() { return costoReparacion; }
    public void setCostoReparacion(BigDecimal costoReparacion) { this.costoReparacion = costoReparacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
}
