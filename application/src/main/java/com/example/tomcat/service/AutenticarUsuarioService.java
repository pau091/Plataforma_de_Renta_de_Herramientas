package com.example.tomcat.service;

import com.example.tomcat.exception.AuthenticationFailedException;
import com.example.tomcat.exception.UsuarioInactivoException;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.AutenticarUsuarioUseCase;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Email;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AutenticarUsuarioService implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final Clock reloj;

    public AutenticarUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort, PasswordHasherPort passwordHasherPort,
                                    Clock reloj) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.reloj = reloj;
    }

    @Override
    public Usuario autenticar(String email, String password) {
        Email correo = new Email(email);
        if (password == null || password.isBlank()) {
            throw new AuthenticationFailedException("Credenciales inválidas.");
        }

        Usuario usuario = usuarioRepositoryPort.buscarPorEmail(correo.getValor())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciales inválidas."));

        // Primero la contraseña: así no se revela si una cuenta existe o está inactiva sin conocer sus credenciales.
        if (!passwordHasherPort.matches(password, usuario.getPassword())) {
            throw new AuthenticationFailedException("Credenciales inválidas.");
        }

        if (!usuario.isActivo()) {
            throw new UsuarioInactivoException();
        }
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        if (usuario.estaSancionado(ahora)) {
            throw new UsuarioInactivoException(usuario.mensajeSancion(ahora, reloj.getZone()));
        }

        return usuario;
    }
}
