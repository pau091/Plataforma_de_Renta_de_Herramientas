package com.example.tomcat.adapter.out.mapper;

import com.example.tomcat.adapter.out.persistence.entity.RolEntity;
import com.example.tomcat.adapter.out.persistence.entity.UsuarioEntity;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioJpaMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        Rol rol = entity.getRoles() == null ? Rol.CLIENTE : entity.getRoles().stream()
                .findFirst()
                .map(RolEntity::getNombre)
                .map(Rol::desde)
                .orElse(Rol.CLIENTE);

        return new Usuario(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                entity.getPassword(),
                entity.isActivo(),
                entity.getCreatedAt(),
                rol,
                entity.getSuspendidoHasta(),
                entity.isBaneado(),
                entity.getMotivoSancion()
        );
    }

    public void copyToEntity(Usuario usuario, UsuarioEntity entity) {
        entity.setNombre(usuario.getNombre());
        entity.setEmail(usuario.getEmail());
        entity.setPassword(usuario.getPassword());
        entity.setActivo(usuario.isActivo());
        entity.setSuspendidoHasta(usuario.getSuspendidoHasta());
        entity.setBaneado(usuario.isBaneado());
        entity.setMotivoSancion(usuario.getMotivoSancion());
        if (usuario.getCreatedAt() != null) {
            entity.setCreatedAt(usuario.getCreatedAt());
        }
    }
}
