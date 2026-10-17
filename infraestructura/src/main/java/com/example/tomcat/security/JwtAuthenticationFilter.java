package com.example.tomcat.security;

import com.example.tomcat.model.Usuario;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo de la petición con el motivo por el que no se autenticó; lo lee {@link JsonSecurityErrorHandler}. */
    public static final String ATRIBUTO_ERROR_AUTH = "auth.error";

    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UsuarioRepositoryPort usuarioRepositoryPort) {
        this.tokenProvider = tokenProvider;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                autenticar(header.substring(7), request);
            }
        } catch (Exception e) {
            logger.error("No se pudo establecer la autenticación del usuario en el contexto", e);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        if (!tokenProvider.validarToken(token)) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "El token es inválido o ha expirado. Inicie sesión nuevamente.");
            return;
        }

        // Se consulta la base de datos para que un usuario desactivado o eliminado pierda el acceso
        // aunque su token aún no haya expirado. El rol también se toma de la base de datos.
        Long usuarioId = tokenProvider.obtenerUsuarioIdDelToken(token);
        Optional<Usuario> usuario = usuarioId == null ? Optional.empty() : usuarioRepositoryPort.buscarPorId(usuarioId);
        if (usuario.isEmpty()) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "El usuario del token ya no existe.");
            return;
        }
        if (!usuario.get().isActivo()) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "El usuario está inactivo. Contacte al administrador de la plataforma.");
            return;
        }

        Usuario u = usuario.get();
        UsuarioAutenticado principal = new UsuarioAutenticado(u.getId(), u.getEmail(), u.getRol());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
