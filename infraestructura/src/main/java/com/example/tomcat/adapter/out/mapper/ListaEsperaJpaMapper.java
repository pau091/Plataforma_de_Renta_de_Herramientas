package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.ListaEsperaEntity;
import com.example.tomcat.adapter.out.persistence.entity.NotificacionEntity;
import com.example.tomcat.model.EntradaListaEspera;
import com.example.tomcat.model.Notificacion;
import org.springframework.stereotype.Component;

/** Convierte las entradas de la lista de espera y las notificaciones entre JPA y el dominio. */
@Component
public class ListaEsperaJpaMapper {

    public EntradaListaEspera toDomain(ListaEsperaEntity e) {
        return new EntradaListaEspera(e.getId(), e.getHerramientaId(), e.getClienteId(), e.getFechaInicio(), e.getFechaFin(),
                e.getEstado(), e.getCreadaEn(), e.getNotificadaEn());
    }

    public ListaEsperaEntity toEntity(EntradaListaEspera d) {
        ListaEsperaEntity e = new ListaEsperaEntity();
        e.setId(d.getId());
        e.setHerramientaId(d.getHerramientaId());
        e.setClienteId(d.getClienteId());
        e.setFechaInicio(d.getFechaInicio());
        e.setFechaFin(d.getFechaFin());
        e.setEstado(d.getEstado());
        e.setCreadaEn(d.getCreadaEn());
        e.setNotificadaEn(d.getNotificadaEn());
        return e;
    }

    public Notificacion toDomain(NotificacionEntity e) {
        return new Notificacion(e.getId(), e.getUsuarioId(), e.getTipo(), e.getTitulo(), e.getMensaje(), e.getHerramientaId(),
                e.isLeida(), e.getCreadaEn());
    }

    public NotificacionEntity toEntity(Notificacion d) {
        NotificacionEntity e = new NotificacionEntity();
        e.setId(d.getId());
        e.setUsuarioId(d.getUsuarioId());
        e.setTipo(d.getTipo());
        e.setTitulo(d.getTitulo());
        e.setMensaje(d.getMensaje());
        e.setHerramientaId(d.getHerramientaId());
        e.setLeida(d.isLeida());
        e.setCreadaEn(d.getCreadaEn());
        return e;
    }
}
