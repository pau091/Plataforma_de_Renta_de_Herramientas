package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.CategoriaEntity;
import com.example.tomcat.model.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaJpaMapper {

    public Categoria toDomain(CategoriaEntity entity) {
        return new Categoria(entity.getId(), entity.getNombre(), entity.getDescripcion());
    }
}
