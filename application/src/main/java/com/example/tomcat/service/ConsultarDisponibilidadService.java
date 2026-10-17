package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.time.LocalDate;
import java.util.List;

public class ConsultarDisponibilidadService implements ConsultarDisponibilidadUseCase {

    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;

    public ConsultarDisponibilidadService(
            HerramientaRepositoryPort herramientaRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort
    ) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
    }

    @Override
    public boolean estaDisponible(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        validarRango(fechaInicio, fechaFin);
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        return herramienta.estaDisponible()
                && !reservaRepositoryPort.existeSolape(herramientaId, fechaInicio, fechaFin, null);
    }

    @Override
    public List<Herramienta> listarDisponiblesEnRango(LocalDate fechaInicio, LocalDate fechaFin) {
        validarRango(fechaInicio, fechaFin);
        return herramientaRepositoryPort.listarDisponibles().stream()
                .filter(herramienta -> !reservaRepositoryPort.existeSolape(herramienta.getId(), fechaInicio, fechaFin, null))
                .toList();
    }

    private void validarRango(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new DomainException("Las fechas de consulta son obligatorias.");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new DomainException("La fecha de inicio no puede ser posterior a la fecha fin.");
        }
    }
}
