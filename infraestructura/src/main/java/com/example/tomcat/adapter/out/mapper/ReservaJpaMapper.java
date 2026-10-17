package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.ReservaEntity;
import com.example.tomcat.model.Reserva;
import org.springframework.stereotype.Component;

@Component
public class ReservaJpaMapper {

    public Reserva toDomain(ReservaEntity entity) {
        return new Reserva(
                entity.getId(),
                entity.getClienteId(),
                entity.getHerramientaId(),
                entity.getFechaInicio(),
                entity.getFechaFin(),
                entity.getEstado(),
                entity.getFechaEntrega(),
                entity.getFechaDevolucion(),
                entity.getEstadoDevolucion(),
                entity.getObservacionesDevolucion()
        );
    }

    public void copyToEntity(Reserva reserva, ReservaEntity entity) {
        entity.setClienteId(reserva.getClienteId());
        entity.setHerramientaId(reserva.getHerramientaId());
        entity.setFechaInicio(reserva.getFechaInicio());
        entity.setFechaFin(reserva.getFechaFin());
        entity.setEstado(reserva.getEstado());
        entity.setFechaEntrega(reserva.getFechaEntrega());
        entity.setFechaDevolucion(reserva.getFechaDevolucion());
        entity.setEstadoDevolucion(reserva.getEstadoDevolucion());
        entity.setObservacionesDevolucion(reserva.getObservacionesDevolucion());
    }
}
