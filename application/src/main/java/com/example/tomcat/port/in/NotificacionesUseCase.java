package com.example.tomcat.port.in;

import com.example.tomcat.model.Notificacion;
import com.example.tomcat.port.in.dto.Solicitante;

import java.util.List;

/** Avisos del usuario autenticado (la campana de la página). */
public interface NotificacionesUseCase {
    List<Notificacion> listar(Solicitante solicitante);
    long contarNoLeidas(Solicitante solicitante);
    Notificacion marcarLeida(Solicitante solicitante, Long id);
    void marcarTodasLeidas(Solicitante solicitante);
}
