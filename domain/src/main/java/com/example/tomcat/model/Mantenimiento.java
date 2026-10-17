package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Mantenimiento {
    private Long id;
    private Long herramientaId;
    private Long reservaId;
    private String descripcionProblema;
    private BigDecimal costoReparacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    public Mantenimiento(Long id, Long herramientaId, Long reservaId, String descripcionProblema,
                         BigDecimal costoReparacion, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        this.id = id;
        this.herramientaId = herramientaId;
        this.reservaId = reservaId;
        this.descripcionProblema = descripcionProblema;
        this.costoReparacion = costoReparacion;
        this.fechaInicio = fechaInicio != null ? fechaInicio : LocalDateTime.now();
        this.fechaFin = fechaFin;
    }

    public Long getId() { return id; }
    public Long getHerramientaId() { return herramientaId; }
    public Long getReservaId() { return reservaId; }
    public String getDescripcionProblema() { return descripcionProblema; }
    public BigDecimal getCostoReparacion() { return costoReparacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }

    public boolean estaAbierto() {
        return fechaFin == null;
    }

    public void cerrar(BigDecimal costoReparacion) {
        if (!estaAbierto()) {
            throw new DomainException("El mantenimiento ya fue cerrado.");
        }
        this.costoReparacion = costoReparacion;
        this.fechaFin = LocalDateTime.now();
    }
}
