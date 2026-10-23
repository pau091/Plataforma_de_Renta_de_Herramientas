package com.example.tomcat.port.in;

import com.example.tomcat.model.Calificacion;
import com.example.tomcat.port.in.dto.ResumenCalificaciones;
import com.example.tomcat.port.in.dto.Solicitante;

import java.util.List;

/** Reseñas de 1 a 5 estrellas que el cliente deja al terminar un alquiler. */
public interface CalificarUseCase {
    Calificacion calificar(Solicitante solicitante, Long reservaId, Integer estrellasHerramienta, Integer estrellasProveedor,
                           String comentario);
    List<Calificacion> listarPorHerramienta(Long herramientaId);
    List<Calificacion> mias(Solicitante solicitante);
    ResumenCalificaciones resumen();
}
