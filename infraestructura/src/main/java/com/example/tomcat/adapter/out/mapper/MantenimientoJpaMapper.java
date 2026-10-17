package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.MantenimientoEntity;
import com.example.tomcat.model.Mantenimiento;
import org.springframework.stereotype.Component;

@Component
public class MantenimientoJpaMapper {

    public Mantenimiento toDomain(MantenimientoEntity entity) {
        return new Mantenimiento(
                entity.getId(),
                entity.getHerramientaId(),
                entity.getReservaId(),
                entity.getDescripcionProblema(),
                entity.getCostoReparacion(),
                entity.getFechaInicio(),
                entity.getFechaFin()
        );
    }

    public void copyToEntity(Mantenimiento mantenimiento, MantenimientoEntity entity) {
        entity.setHerramientaId(mantenimiento.getHerramientaId());
        entity.setReservaId(mantenimiento.getReservaId());
        entity.setDescripcionProblema(mantenimiento.getDescripcionProblema());
        entity.setCostoReparacion(mantenimiento.getCostoReparacion());
        if (mantenimiento.getFechaInicio() != null) {
            entity.setFechaInicio(mantenimiento.getFechaInicio());
        }
        entity.setFechaFin(mantenimiento.getFechaFin());
    }
}
