package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.CalificacionEntity;
import com.example.tomcat.model.Calificacion;
import org.springframework.stereotype.Component;

@Component
public class CalificacionJpaMapper {

    public Calificacion toDomain(CalificacionEntity entity) {
        return new Calificacion(
                entity.getId(),
                entity.getReservaId(),
                entity.getClienteId(),
                entity.getClienteNombre(),
                entity.getHerramientaId(),
                entity.getProveedorId(),
                entity.getEstrellasHerramienta(),
                entity.getEstrellasProveedor(),
                entity.getComentario(),
                entity.getFecha()
        );
    }

    public CalificacionEntity toEntity(Calificacion calificacion) {
        CalificacionEntity entity = new CalificacionEntity();
        entity.setId(calificacion.getId());
        entity.setReservaId(calificacion.getReservaId());
        entity.setClienteId(calificacion.getClienteId());
        entity.setClienteNombre(calificacion.getClienteNombre());
        entity.setHerramientaId(calificacion.getHerramientaId());
        entity.setProveedorId(calificacion.getProveedorId());
        entity.setEstrellasHerramienta(calificacion.getEstrellasHerramienta());
        entity.setEstrellasProveedor(calificacion.getEstrellasProveedor());
        entity.setComentario(calificacion.getComentario());
        entity.setFecha(calificacion.getFecha());
        return entity;
    }
}
