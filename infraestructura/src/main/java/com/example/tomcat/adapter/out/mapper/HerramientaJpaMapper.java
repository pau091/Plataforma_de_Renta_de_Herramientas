package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.HerramientaEntity;
import com.example.tomcat.model.Herramienta;
import org.springframework.stereotype.Component;

@Component
public class HerramientaJpaMapper {

    public Herramienta toDomain(HerramientaEntity entity) {
        return new Herramienta(
                entity.getId(),
                entity.getProveedorId(),
                entity.getCategoriaId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getTarifaDiaria(),
                entity.getEstado(),
                entity.getImagenUrl()
        );
    }

    public void copyToEntity(Herramienta herramienta, HerramientaEntity entity) {
        entity.setProveedorId(herramienta.getProveedorId());
        entity.setCategoriaId(herramienta.getCategoriaId());
        entity.setNombre(herramienta.getNombre());
        entity.setDescripcion(herramienta.getDescripcion());
        entity.setTarifaDiaria(herramienta.getTarifaDiaria());
        entity.setEstado(herramienta.getEstado());
        entity.setImagenUrl(herramienta.getImagenUrl());
    }
}
