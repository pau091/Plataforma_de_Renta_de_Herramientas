package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Email;

import java.util.List;

public class GestionarUsuarioService implements GestionarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public GestionarUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public List<Usuario> listarClientes() {
        return usuarioRepositoryPort.listarPorRol(Rol.CLIENTE);
    }

    @Override
    public List<Usuario> listarProveedores() {
        return usuarioRepositoryPort.listarPorRol(Rol.PROVEEDOR);
    }

    @Override
    public Usuario buscarPorId(Long id) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("El usuario no existe."));
        exigirClienteOProveedor(usuario);
        return usuario;
    }

    @Override
    public Usuario actualizar(Long id, String nombre, String email, Boolean activo, String password) {
        Usuario usuario = buscarPorId(id);

        String nombreActualizado = (nombre == null || nombre.isBlank()) ? usuario.getNombre() : nombre.trim();
        String emailActualizado = usuario.getEmail();
        if (email != null && !email.isBlank()) {
            emailActualizado = new Email(email).getValor();
            usuarioRepositoryPort.buscarPorEmail(emailActualizado)
                    .filter(existente -> !existente.getId().equals(id))
                    .ifPresent(existente -> {
                        throw new DomainException("El correo ya se encuentra registrado.");
                    });
        }

        usuario.actualizarDatos(nombreActualizado, emailActualizado);

        if (activo != null) {
            if (activo) {
                usuario.activar();
            } else {
                usuario.desactivar();
            }
        }
        if (password != null && !password.isBlank()) {
            if (password.length() < 8) {
                throw new DomainException("La contraseña debe tener al menos 8 caracteres.");
            }
            usuario.cambiarPassword(passwordHasherPort.hash(password));
        }

        return usuarioRepositoryPort.guardar(usuario);
    }

    @Override
    public void eliminar(Long id) {
        buscarPorId(id);
        usuarioRepositoryPort.eliminar(id);
    }

    private void exigirClienteOProveedor(Usuario usuario) {
        if (usuario.getRol() != Rol.CLIENTE && usuario.getRol() != Rol.PROVEEDOR) {
            throw new DomainException("Solo se pueden gestionar clientes y proveedores.");
        }
    }
}
