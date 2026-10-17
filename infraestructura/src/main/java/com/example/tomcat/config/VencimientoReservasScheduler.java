package com.example.tomcat.config;

import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.VencerReservasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vence automáticamente las reservas que no se concretaron a tiempo: PENDIENTE sin aceptar o ACEPTADA
 * sin pagar dentro de app.reservas.plazo-maximo (1 hora), o cuya fecha de inicio ya pasó.
 * Se ejecuta al arrancar la aplicación y luego según app.vencimiento.cron (por defecto, cada minuto).
 */
@Component
@EnableScheduling
public class VencimientoReservasScheduler {

    private static final Logger log = LoggerFactory.getLogger(VencimientoReservasScheduler.class);

    private final VencerReservasUseCase vencerReservasUseCase;

    public VencimientoReservasScheduler(VencerReservasUseCase vencerReservasUseCase) {
        this.vencerReservasUseCase = vencerReservasUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        ejecutar();
    }

    @Scheduled(cron = "${app.vencimiento.cron}", zone = "${app.zona-horaria}")
    public void ejecutar() {
        List<Reserva> vencidas = vencerReservasUseCase.vencerReservas();
        if (!vencidas.isEmpty()) {
            log.info("Reservas vencidas automáticamente: {}", vencidas.stream().map(Reserva::getId).toList());
        }
    }
}
