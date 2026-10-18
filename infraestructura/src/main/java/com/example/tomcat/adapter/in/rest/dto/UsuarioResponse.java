package com.example.tomcat.adapter.in.rest.dto;

import com.example.tomcat.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Datos públicos de un usuario (nunca expone la contraseña). */
public record UsuarioResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "Juan Cliente") String nombre,
        @Schema(example = "juan.cliente@gmail.com") String email,
        @Schema(example = "true") boolean activo,
        @Schema(example = "CLIENTE", allowableValues = {"ADMIN", "PROVEEDOR", "CLIENTE"}) String rol,
        @Schema(type = "string", example = "2026-10-17T21:08:15", description = "Fecha de registro (UTC)") LocalDateTime createdAt
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
