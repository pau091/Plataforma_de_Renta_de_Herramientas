package com.example.tomcat.service;

import com.example.tomcat.model.Reserva;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Reloj y plazo de respuesta de las reservas.
 * - "Hoy" se calcula en la zona horaria del negocio (fechas de inicio y fin de una reserva).
 * - Los instantes (creación, aceptación) se registran y comparan en UTC, igual que en la base de datos.
 * - Una reserva PENDIENTE o ACEPTADA sin pagar tiene como máximo {@code plazoMaximo} para avanzar.
 */
public class PoliticaVencimiento {

    private final Clock reloj;
    private final Duration plazoMaximo;

    public PoliticaVencimiento(Clock reloj, Duration plazoMaximo) {
        this.reloj = reloj;
        this.plazoMaximo = plazoMaximo;
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public LocalDateTime ahoraUtc() {
        return LocalDateTime.now(reloj.withZone(ZoneOffset.UTC));
    }

    public boolean estaVencida(Reserva reserva) {
        return reserva.debeVencer(hoy(), ahoraUtc().minus(plazoMaximo));
    }

    public Duration getPlazoMaximo() {
        return plazoMaximo;
    }
}
