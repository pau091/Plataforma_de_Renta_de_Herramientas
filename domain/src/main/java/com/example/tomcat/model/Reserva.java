package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class Reserva {
    private static final Set<String> ESTADOS_DEVOLUCION = Set.of("BUENO", "DANADO", "PERDIDO");

    private Long id;
    private Long clienteId;
    private Long herramientaId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaDevolucion;
    private String estadoDevolucion;
    private String observacionesDevolucion;

    public Reserva(Long id, Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, String estado,
                   LocalDateTime fechaEntrega, LocalDateTime fechaDevolucion, String estadoDevolucion,
                   String observacionesDevolucion) {
        this.id = id;
        this.clienteId = clienteId;
        this.herramientaId = herramientaId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado != null ? estado : "PENDIENTE";
        this.fechaEntrega = fechaEntrega;
        this.fechaDevolucion = fechaDevolucion;
        this.estadoDevolucion = estadoDevolucion;
        this.observacionesDevolucion = observacionesDevolucion;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public Long getHerramientaId() { return herramientaId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public LocalDateTime getFechaDevolucion() { return fechaDevolucion; }
    public String getEstadoDevolucion() { return estadoDevolucion; }
    public String getObservacionesDevolucion() { return observacionesDevolucion; }

    public long diasAlquiler() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
    }

    public void aceptar() {
        exigirEstado("PENDIENTE", "Solo se pueden aceptar reservas pendientes.");
        this.estado = "ACEPTADA";
    }

    public void rechazar() {
        exigirEstado("PENDIENTE", "Solo se pueden rechazar reservas pendientes.");
        this.estado = "RECHAZADA";
    }

    public void entregar() {
        exigirEstado("ACEPTADA", "Solo se pueden entregar reservas aceptadas.");
        this.estado = "ENTREGADA";
        this.fechaEntrega = LocalDateTime.now();
    }

    public void devolver(String estadoEquipo, String observaciones) {
        exigirEstado("ENTREGADA", "Solo se pueden devolver herramientas ya entregadas.");
        String estadoNormalizado = normalizarEstadoDevolucion(estadoEquipo);
        if (!ESTADOS_DEVOLUCION.contains(estadoNormalizado)) {
            throw new DomainException("Estado de devolución no válido. Use BUENO, DANADO o PERDIDO.");
        }
        this.estadoDevolucion = estadoNormalizado;
        this.observacionesDevolucion = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        this.fechaDevolucion = LocalDateTime.now();
        this.estado = "FINALIZADA";
    }

    /**
     * Una reserva vence cuando llega su fecha de inicio sin haberse concretado: el proveedor nunca
     * la respondió (PENDIENTE) o fue aceptada pero no se pagó. Al vencer libera esas fechas.
     */
    public void vencer() {
        if (!"PENDIENTE".equals(estado) && !"ACEPTADA".equals(estado)) {
            throw new DomainException("Solo pueden vencer reservas pendientes o aceptadas sin entregar.");
        }
        this.estado = "VENCIDA";
    }

    public boolean permitePago() {
        return "ACEPTADA".equals(estado) || "ENTREGADA".equals(estado) || "FINALIZADA".equals(estado);
    }

    public boolean tieneDano() {
        return "DANADO".equals(estadoDevolucion);
    }

    public boolean estaPerdida() {
        return "PERDIDO".equals(estadoDevolucion);
    }

    private String normalizarEstadoDevolucion(String estadoEquipo) {
        if (estadoEquipo == null || estadoEquipo.isBlank()) {
            throw new DomainException("El estado del equipo al devolverlo es obligatorio.");
        }
        String valor = estadoEquipo.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N");
        if ("DAÑADO".equalsIgnoreCase(estadoEquipo.trim()) || "DAÑADO".equals(valor) || "DANADO".equals(valor)) {
            return "DANADO";
        }
        return valor;
    }

    private void exigirEstado(String esperado, String mensaje) {
        if (!esperado.equals(this.estado)) {
            throw new DomainException(mensaje);
        }
    }
}
