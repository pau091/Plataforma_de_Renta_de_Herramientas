package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Ciclo de vida de una reserva:
 * PENDIENTE --(proveedor acepta)--> ACEPTADA --(cliente paga: confirmación automática)--> CONFIRMADA
 * --(proveedor entrega)--> ENTREGADA --(devolución)--> FINALIZADA.
 * PENDIENTE puede pasar a RECHAZADA; PENDIENTE y ACEPTADA pueden pasar a VENCIDA si se agota el plazo.
 * El cliente puede cancelar una reserva PENDIENTE o ACEPTADA (CANCELADA); una CONFIRMADA solo se cancela
 * cuando el administrador reembolsa su pago antes de la entrega.
 * Al pagar se cobra el depósito de garantía ({@link Garantia}); al devolver se liquida, descontando la multa por
 * retraso (si se devuelve después de la fecha fin) y los daños.
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
    private Garantia garantia;

    public Reserva(Long id, Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, String estado,
                   LocalDateTime fechaEntrega, LocalDateTime fechaDevolucion, String estadoDevolucion,
                   String observacionesDevolucion, LocalDateTime fechaCreacion, LocalDateTime fechaAceptacion) {
        this(id, clienteId, herramientaId, fechaInicio, fechaFin, estado, fechaEntrega, fechaDevolucion, estadoDevolucion,
                observacionesDevolucion, fechaCreacion, fechaAceptacion, null);
    }

    public Reserva(Long id, Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, String estado,
                   LocalDateTime fechaEntrega, LocalDateTime fechaDevolucion, String estadoDevolucion,
                   String observacionesDevolucion, LocalDateTime fechaCreacion, LocalDateTime fechaAceptacion,
                   Garantia garantia) {
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
        this.garantia = garantia != null ? garantia : Garantia.sinCobrar();
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
    public Garantia getGarantia() { return garantia; }

    public long diasAlquiler() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
    }

    /** Días que pasaron desde la fecha fin hasta {@code dia}; 0 si se devuelve a tiempo. */
    public long diasRetraso(LocalDate dia) {
        return Math.max(0, ChronoUnit.DAYS.between(fechaFin, dia));
    }

    /** Multa por devolución tardía: cada día de retraso cuesta la tarifa diaria más el recargo (0,5 = 50%). */
    public static BigDecimal calcularMulta(long diasRetraso, BigDecimal tarifaDiaria, BigDecimal recargoMora) {
        if (diasRetraso <= 0) {
            return BigDecimal.ZERO;
        }
        return tarifaDiaria.multiply(BigDecimal.ONE.add(recargoMora))
                .multiply(BigDecimal.valueOf(diasRetraso))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /** Reglas de fechas comunes al crear y al modificar una reserva. */
    public static void validarFechas(LocalDate fechaInicio, LocalDate fechaFin, LocalDate hoy) {
        if (fechaInicio == null || fechaFin == null) {
            throw new DomainException("Las fechas de la reserva son obligatorias.");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new DomainException("La fecha de inicio no puede ser posterior a la fecha fin.");
        }
        if (fechaInicio.isBefore(hoy)) {
            throw new DomainException("No se puede reservar con una fecha de inicio pasada.");
        }
    }

    /** El cliente solo puede cambiar las fechas mientras el proveedor no haya respondido. */
    public void modificarFechas(LocalDate nuevaFechaInicio, LocalDate nuevaFechaFin, LocalDate hoy) {
        exigirEstado("PENDIENTE", "Solo se pueden modificar las fechas de reservas pendientes.");
        validarFechas(nuevaFechaInicio, nuevaFechaFin, hoy);
        this.fechaInicio = nuevaFechaInicio;
        this.fechaFin = nuevaFechaFin;
    }

    /** Cancelación por parte del cliente (o un admin) antes de pagar. Libera las fechas. */
    public void cancelar() {
        if ("CONFIRMADA".equals(estado)) {
            throw new DomainException("La reserva ya está pagada: para cancelarla el administrador debe reembolsar el pago.");
        }
        if (!"PENDIENTE".equals(estado) && !"ACEPTADA".equals(estado)) {
            throw new DomainException("Solo se pueden cancelar reservas pendientes o aceptadas sin pagar.");
        }
        this.estado = "CANCELADA";
    }

    /** Cancelación de una reserva pagada a causa del reembolso de su pago (antes de entregar la herramienta). */
    public void cancelarPorReembolso() {
        exigirEstado("CONFIRMADA", "Solo se puede reembolsar el pago de reservas confirmadas que aún no se han entregado.");
        this.estado = "CANCELADA";
        this.garantia = garantia.reembolsada();
    }

    public void aceptar(LocalDateTime ahora) {
        exigirEstado("PENDIENTE", "Solo se pueden aceptar reservas pendientes.");
        this.estado = "ACEPTADA";
        this.fechaAceptacion = ahora;
    }

    /** Confirmación automática al pagar una reserva aceptada; el depósito de garantía queda retenido. */
    public void confirmarPago(BigDecimal deposito) {
        exigirEstado("ACEPTADA", "La reserva debe estar aceptada por el proveedor para poder pagarla.");
        this.estado = "CONFIRMADA";
        this.garantia = Garantia.retenida(deposito);
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

    /**
     * Registra la devolución y liquida el depósito: si se devuelve después de la fecha fin, cada día de retraso genera
     * una multa ({@link #calcularMulta}); la multa y los daños se descuentan del depósito.
     *
     * @param hoy          día de la devolución (zona del negocio)
     * @param tarifaDiaria tarifa de la herramienta, base de la multa
     * @param recargoMora  recargo sobre la tarifa por día de retraso (0,5 = 50%)
     */
    public void devolver(String estadoEquipo, String observaciones, LocalDate hoy, BigDecimal tarifaDiaria,
                         BigDecimal recargoMora) {
        exigirEstado("ENTREGADA", "Solo se pueden devolver herramientas ya entregadas.");
        String estadoNormalizado = normalizarEstadoDevolucion(estadoEquipo);
        if (!ESTADOS_DEVOLUCION.contains(estadoNormalizado)) {
            throw new DomainException("Estado de devolución no válido. Use BUENO, DANADO o PERDIDO.");
        }
        long dias = diasRetraso(hoy);
        // Reservas pagadas antes de existir el depósito no tienen garantía registrada: se liquidan sin depósito.
        Garantia base = garantia.estado() == null ? Garantia.retenida(BigDecimal.ZERO) : garantia;
        this.garantia = base.liquidarDevolucion(estadoNormalizado, dias, calcularMulta(dias, tarifaDiaria, recargoMora));
        this.estadoDevolucion = estadoNormalizado;
        this.observacionesDevolucion = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        this.fechaDevolucion = LocalDateTime.now();
        this.estado = "FINALIZADA";
    }

    /** Se cerró la reparación de una devolución con daño: el costo real se descuenta del depósito. */
    public void liquidarReparacion(BigDecimal costoReparacion) {
        this.garantia = garantia.liquidarReparacion(costoReparacion);
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
