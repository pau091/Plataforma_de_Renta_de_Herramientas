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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo de la petición con el motivo por el que no se autenticó; lo lee {@link JsonSecurityErrorHandler}. */
    public static final String ATRIBUTO_ERROR_AUTH = "auth.error";

    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ListaNegraTokensAdapter listaNegra;
    private final Clock reloj;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UsuarioRepositoryPort usuarioRepositoryPort,
                                   ListaNegraTokensAdapter listaNegra, Clock reloj) {
        this.tokenProvider = tokenProvider;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.listaNegra = listaNegra;
        this.reloj = reloj;
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
        // 1. Firma y vigencia.
        Optional<JwtTokenProvider.DatosToken> datos = tokenProvider.leer(token);
        if (datos.isEmpty()) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "El token es inválido o ha expirado. Inicie sesión nuevamente.");
            return;
        }
        if (datos.get().jti() == null) {
            // Token emitido antes de existir la lista negra: no se podría revocar, así que no se acepta.
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "Su sesión es anterior a una actualización de seguridad. Inicie sesión nuevamente.");
            return;
        }

        // 2. Lista negra: un token revocado (suspensión, baneo, desactivación o cierre de sesión) ya no sirve,
        //    aunque su firma sea válida y no haya expirado.
        Optional<String> revocado = listaNegra.motivoRevocacion(datos.get().jti());
        if (revocado.isPresent()) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "Su sesión fue revocada. " + revocado.get() + ".");
            return;
        }

        // 3. Se consulta la base de datos para que un usuario desactivado o eliminado pierda el acceso
        //    aunque su token aún no haya expirado. El rol también se toma de la base de datos.
        Long usuarioId = datos.get().usuarioId();
        Optional<Usuario> usuario = usuarioId == null ? Optional.empty() : usuarioRepositoryPort.buscarPorId(usuarioId);
        if (usuario.isEmpty()) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "El usuario del token ya no existe.");
            return;
        }
        if (!usuario.get().isActivo()) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, "El usuario está inactivo. Contacte al administrador de la plataforma.");
            return;
        }
        // 4. Defensa adicional: una cuenta suspendida o baneada tampoco entra, aunque el token no estuviera en la lista.
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        if (usuario.get().estaSancionado(ahora)) {
            request.setAttribute(ATRIBUTO_ERROR_AUTH, usuario.get().mensajeSancion(ahora, reloj.getZone()));
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
