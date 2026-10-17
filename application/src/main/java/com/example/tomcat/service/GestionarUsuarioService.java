package com.example.tomcat.service;

import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Email;

import java.util.List;

public class GestionarUsuarioService implements GestionarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public GestionarUsuarioService(
            UsuarioRepositoryPort usuarioRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            PasswordHasherPort passwordHasherPort
    ) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
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
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
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
                        throw new ConflictoException("El correo ya se encuentra registrado.");
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

    /**
     * Solo se eliminan usuarios sin historial. Un cliente con reservas o un proveedor con herramientas
     * se desactiva en su lugar, para no perder reservas, pagos ni facturas.
     */
    @Override
    public void eliminar(Long id) {
        Usuario usuario = buscarPorId(id);
        boolean tieneHistorial = usuario.getRol() == Rol.CLIENTE
                ? !reservaRepositoryPort.listarPorCliente(id).isEmpty()
                : !herramientaRepositoryPort.listarPorProveedor(id).isEmpty();
        if (tieneHistorial) {
            throw new ConflictoException(usuario.getRol() == Rol.CLIENTE
                    ? "El cliente tiene reservas registradas y no se puede eliminar. Desactívelo con activo=false."
                    : "El proveedor tiene herramientas registradas y no se puede eliminar. Desactívelo con activo=false.");
        }
        usuarioRepositoryPort.eliminar(id);
    }

    private void exigirClienteOProveedor(Usuario usuario) {
        if (usuario.getRol() != Rol.CLIENTE && usuario.getRol() != Rol.PROVEEDOR) {
            throw new DomainException("Solo se pueden gestionar clientes y proveedores.");
        }
    }
}
