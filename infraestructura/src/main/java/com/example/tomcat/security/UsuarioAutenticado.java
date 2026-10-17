package com.example.tomcat.security;

import com.example.tomcat.model.Rol;
import com.example.tomcat.port.in.dto.Solicitante;

/**
 * Principal que el {@link JwtAuthenticationFilter} guarda en el SecurityContext.
 * Los controllers lo reciben con {@code @AuthenticationPrincipal UsuarioAutenticado usuario}.
 */
public record UsuarioAutenticado(Long id, String email, Rol rol) {

    public Solicitante solicitante() {
        return new Solicitante(id, rol);
    }
}
