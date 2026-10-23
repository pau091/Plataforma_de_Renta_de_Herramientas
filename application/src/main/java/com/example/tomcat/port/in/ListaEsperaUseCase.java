package com.example.tomcat.port.in;

import com.example.tomcat.model.EntradaListaEspera;
import com.example.tomcat.port.in.dto.Solicitante;

import java.time.LocalDate;
import java.util.List;

/** Anotarse a la espera de unas fechas ocupadas, consultar y salir de la lista. */
public interface ListaEsperaUseCase {
    EntradaListaEspera anotarse(Solicitante solicitante, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin);
    List<EntradaListaEspera> mias(Solicitante solicitante);
    EntradaListaEspera cancelar(Solicitante solicitante, Long id);
}
