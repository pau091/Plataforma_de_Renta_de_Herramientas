package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Ciclo de vida de una reserva:
 * PENDIENTE --(proveedor acepta)--> ACEPTADA --(cliente paga: confirmación automática)--> CONFIRMADA
 * --(proveedor entrega)--> ENTREGADA --(devolución)--> FINALIZADA.
 * PENDIENTE puede pasar a RECHAZADA; PENDIENTE y ACEPTADA pueden pasar a VENCIDA si se agota el plazo.
 */
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
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAceptacion;

    public Reserva(Long id, Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, String estado,
                   LocalDateTime fechaEntrega, LocalDateTime fechaDevolucion, String estadoDevolucion,
                   String observacionesDevolucion, LocalDateTime fechaCreacion, LocalDateTime fechaAceptacion) {
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
        this.fechaCreacion = fechaCreacion;
        this.fechaAceptacion = fechaAceptacion;
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
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaAceptacion() { return fechaAceptacion; }

    public long diasAlquiler() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
    }

    public void aceptar(LocalDateTime ahora) {
        exigirEstado("PENDIENTE", "Solo se pueden aceptar reservas pendientes.");
        this.estado = "ACEPTADA";
        this.fechaAceptacion = ahora;
    }

    /** Confirmación automática: ocurre en el momento en que el cliente paga una reserva aceptada. */
    public void confirmarPago() {
        exigirEstado("ACEPTADA", "La reserva debe estar aceptada por el proveedor para poder pagarla.");
        this.estado = "CONFIRMADA";
    }

    public void rechazar() {
        exigirEstado("PENDIENTE", "Solo se pueden rechazar reservas pendientes.");
        this.estado = "RECHAZADA";
    }

    public void entregar() {
        exigirEstado("CONFIRMADA", "Solo se pueden entregar reservas confirmadas (aceptadas y pagadas).");
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
     * Indica si la reserva debe vencer porque no se concretó a tiempo:
     * - PENDIENTE: el proveedor no la aceptó dentro del plazo desde su creación.
     * - ACEPTADA: el cliente no la pagó dentro del plazo desde su aceptación.
     * - En ambos casos, también vence si su fecha de inicio ya pasó.
     *
     * @param hoy             fecha actual del negocio
     * @param limiteRespuesta instante (UTC) antes del cual se considera agotado el plazo: ahora - plazo máximo
     */
    public boolean debeVencer(LocalDate hoy, LocalDateTime limiteRespuesta) {
        if (!"PENDIENTE".equals(estado) && !"ACEPTADA".equals(estado)) {
            return false;
        }
        if (fechaInicio.isBefore(hoy)) {
            return true;
        }
        LocalDateTime referencia = "ACEPTADA".equals(estado) && fechaAceptacion != null ? fechaAceptacion : fechaCreacion;
        return referencia != null && !referencia.isAfter(limiteRespuesta);
    }

    public void vencer() {
        if (!"PENDIENTE".equals(estado) && !"ACEPTADA".equals(estado)) {
            throw new DomainException("Solo pueden vencer reservas pendientes o aceptadas sin pagar.");
        }
        this.estado = "VENCIDA";
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
