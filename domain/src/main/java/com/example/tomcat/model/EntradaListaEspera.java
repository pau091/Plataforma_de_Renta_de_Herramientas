package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cliente que espera unas fechas ocupadas de una herramienta. Si la reserva que las ocupa se cancela, se rechaza,
 * vence o se reembolsa, se notifica al primero de la lista (por orden de llegada) cuyas fechas quedaron libres.
 * ESPERANDO → NOTIFICADA (ya puede reservar) → ATENDIDA (reservó); en cualquier momento puede pasar a CANCELADA.
 */
public class EntradaListaEspera {

    private Long id;
    private Long herramientaId;
    private Long clienteId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
    private LocalDateTime creadaEn;
    private LocalDateTime notificadaEn;

    public EntradaListaEspera(Long id, Long herramientaId, Long clienteId, LocalDate fechaInicio, LocalDate fechaFin,
                              String estado, LocalDateTime creadaEn, LocalDateTime notificadaEn) {
        this.id = id;
        this.herramientaId = herramientaId;
        this.clienteId = clienteId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado != null ? estado : "ESPERANDO";
        this.creadaEn = creadaEn;
        this.notificadaEn = notificadaEn;
    }

    /** Se anota en la fila con las mismas reglas de fechas que una reserva. */
    public static EntradaListaEspera nueva(Long herramientaId, Long clienteId, LocalDate fechaInicio, LocalDate fechaFin,
                                           LocalDate hoy, LocalDateTime ahoraUtc) {
        Reserva.validarFechas(fechaInicio, fechaFin, hoy);
        return new EntradaListaEspera(null, herramientaId, clienteId, fechaInicio, fechaFin, "ESPERANDO", ahoraUtc, null);
    }

    public Long getId() { return id; }
    public Long getHerramientaId() { return herramientaId; }
    public Long getClienteId() { return clienteId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public String getEstado() { return estado; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getNotificadaEn() { return notificadaEn; }

    /** Sigue en la fila: esperando o ya avisado pero sin reservar todavía. */
    public boolean activa() {
        return "ESPERANDO".equals(estado) || "NOTIFICADA".equals(estado);
    }

    public boolean seCruzaCon(LocalDate inicio, LocalDate fin) {
        return !fechaInicio.isAfter(fin) && !fechaFin.isBefore(inicio);
    }

    public void notificar(LocalDateTime ahoraUtc) {
        if (!"ESPERANDO".equals(estado)) {
            throw new DomainException("Solo se notifica a quien sigue esperando.");
        }
        this.estado = "NOTIFICADA";
        this.notificadaEn = ahoraUtc;
    }

    /** Terminó reservando las fechas que esperaba. */
    public void atender() {
        if (!activa()) {
            throw new DomainException("La entrada de la lista de espera ya no está activa.");
        }
        this.estado = "ATENDIDA";
    }

    public void cancelar() {
        if (!activa()) {
            throw new DomainException("La entrada de la lista de espera ya no está activa.");
        }
        this.estado = "CANCELADA";
    }
}
