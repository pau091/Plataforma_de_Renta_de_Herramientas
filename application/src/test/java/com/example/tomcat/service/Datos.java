package com.example.tomcat.service;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.dto.Solicitante;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Datos de prueba compartidos: reloj fijo y entidades típicas. */
final class Datos {

    /** 17/10/2026 a las 15:00 en Bogotá (20:00 UTC). */
    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-17T20:00:00Z"), ZoneId.of("America/Bogota"));
    static final LocalDate HOY = LocalDate.of(2026, 10, 17);
    static final LocalDateTime AHORA_UTC = LocalDateTime.of(2026, 10, 17, 20, 0);
    static final PoliticaVencimiento POLITICA = new PoliticaVencimiento(RELOJ, Duration.ofHours(1));

    static final Long ADMIN_ID = 1L;
    static final Long PROVEEDOR_ID = 2L;
    static final Long OTRO_PROVEEDOR_ID = 3L;
    static final Long CLIENTE_ID = 12L;
    static final Long OTRO_CLIENTE_ID = 13L;
    static final Long HERRAMIENTA_ID = 1L;

    static final Solicitante ADMIN = new Solicitante(ADMIN_ID, Rol.ADMIN);
    static final Solicitante PROVEEDOR = new Solicitante(PROVEEDOR_ID, Rol.PROVEEDOR);
    static final Solicitante OTRO_PROVEEDOR = new Solicitante(OTRO_PROVEEDOR_ID, Rol.PROVEEDOR);
    static final Solicitante CLIENTE = new Solicitante(CLIENTE_ID, Rol.CLIENTE);
    static final Solicitante OTRO_CLIENTE = new Solicitante(OTRO_CLIENTE_ID, Rol.CLIENTE);

    private Datos() {
    }

    /** Herramienta del PROVEEDOR con tarifa de 45.000 por día. */
    static Herramienta herramienta(String estado) {
        return new Herramienta(HERRAMIENTA_ID, PROVEEDOR_ID, 1L, "Taladro Percutor", "800W", new BigDecimal("45000"), estado, null);
    }

    /** Reserva del CLIENTE sobre la herramienta, del día +5 al +7 (3 días), creada hace 10 minutos. */
    static Reserva reserva(Long id, String estado) {
        LocalDateTime aceptacion = "PENDIENTE".equals(estado) ? null : AHORA_UTC.minusMinutes(5);
        return new Reserva(id, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(5), HOY.plusDays(7), estado,
                null, null, null, null, AHORA_UTC.minusMinutes(10), aceptacion);
    }

    static Pago pago(Long id, Long reservaId, String monto, String estado) {
        return new Pago(id, reservaId, new BigDecimal(monto), "TARJETA", estado, AHORA_UTC);
    }

    static Usuario usuario(Long id, String nombre, Rol rol, boolean activo) {
        return new Usuario(id, nombre, nombre.toLowerCase().replace(' ', '.') + "@correo.com", "hash", activo, null, rol);
    }
}
