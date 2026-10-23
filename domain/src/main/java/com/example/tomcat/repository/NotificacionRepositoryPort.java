package com.example.tomcat.repository;

import com.example.tomcat.model.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepositoryPort {
    Notificacion guardar(Notificacion notificacion);
    Optional<Notificacion> buscarPorId(Long id);
    /** Más recientes primero. */
    List<Notificacion> listarPorUsuario(Long usuarioId);
    long contarNoLeidas(Long usuarioId);
    void marcarTodasLeidas(Long usuarioId);
}
