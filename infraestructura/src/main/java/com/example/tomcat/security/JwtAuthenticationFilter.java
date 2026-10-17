package com.example.tomcat.security;

import com.example.tomcat.model.Rol;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (tokenProvider.validarToken(token)) {
                    String email = tokenProvider.obtenerEmailDelToken(token);
                    Rol rol = tokenProvider.obtenerRolDelToken(token);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            logger.error("No se pudo establecer la autenticación del usuario en el contexto", e);
        }
        filterChain.doFilter(request, response);
    }
}
