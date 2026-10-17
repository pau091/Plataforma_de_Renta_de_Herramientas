package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Email;

public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegistrarUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public Usuario registrar(String nombre, String email, String password, String rol) {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("El nombre es obligatorio.");
        }
        Email correo = new Email(email);
        if (password == null || password.length() < 8) {
            throw new DomainException("La contraseña debe tener al menos 8 caracteres.");
        }

        Rol rolUsuario = Rol.desde(rol);
        if (rolUsuario == Rol.ADMIN) {
            throw new DomainException("No está permitido registrar un administrador por este medio.");
        }

        if (usuarioRepositoryPort.buscarPorEmail(correo.getValor()).isPresent()) {
            throw new DomainException("El correo ya se encuentra registrado.");
        }

        Usuario nuevoUsuario = new Usuario(
                null,
                nombre.trim(),
                correo.getValor(),
                passwordHasherPort.hash(password),
                true,
                null,
                rolUsuario
        );
        return usuarioRepositoryPort.guardar(nuevoUsuario);
    }
}
