package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Notificacion;
import com.example.tomcat.port.in.NotificacionesUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.NotificacionRepositoryPort;

import java.util.List;

public class NotificacionesService implements NotificacionesUseCase {

    private final NotificacionRepositoryPort notificacionRepositoryPort;

    public NotificacionesService(NotificacionRepositoryPort notificacionRepositoryPort) {
        this.notificacionRepositoryPort = notificacionRepositoryPort;
    }

    @Override
    public List<Notificacion> listar(Solicitante solicitante) {
        return notificacionRepositoryPort.listarPorUsuario(solicitante.id());
    }

    @Override
    public long contarNoLeidas(Solicitante solicitante) {
        return notificacionRepositoryPort.contarNoLeidas(solicitante.id());
    }

    @Override
    public Notificacion marcarLeida(Solicitante solicitante, Long id) {
        Notificacion notificacion = notificacionRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificación no existe."));
        if (!solicitante.esUsuario(notificacion.getUsuarioId())) {
            throw new AccesoDenegadoException("La notificación es de otro usuario.");
        }
        notificacion.marcarLeida();
        return notificacionRepositoryPort.guardar(notificacion);
    }

    @Override
    public void marcarTodasLeidas(Solicitante solicitante) {
        notificacionRepositoryPort.marcarTodasLeidas(solicitante.id());
    }
}
