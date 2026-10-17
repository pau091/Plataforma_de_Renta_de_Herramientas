package com.example.tomcat.port.in;

import com.example.tomcat.model.Herramienta;

import java.time.LocalDate;
import java.util.List;

public interface ConsultarDisponibilidadUseCase {
    boolean estaDisponible(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin);
    List<Herramienta> listarDisponiblesEnRango(LocalDate fechaInicio, LocalDate fechaFin);
}
