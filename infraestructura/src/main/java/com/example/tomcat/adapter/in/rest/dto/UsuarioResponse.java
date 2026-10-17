package com.example.tomcat.adapter.in.rest.dto;

import com.example.tomcat.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        boolean activo,
        String rol,
        LocalDateTime createdAt
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.isActivo(),
                usuario.getRol().name(),
                usuario.getCreatedAt()
        );
    }
}
