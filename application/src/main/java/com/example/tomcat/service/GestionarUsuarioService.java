package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
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
    /** Correo del administrador principal (app.admin-principal): el único que reasigna roles. */
    private final String emailAdminPrincipal;

    public GestionarUsuarioService(
            UsuarioRepositoryPort usuarioRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            String emailAdminPrincipal
    ) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.emailAdminPrincipal = emailAdminPrincipal;
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
    public List<Usuario> listarAdministradores() {
        return usuarioRepositoryPort.listarPorRol(Rol.ADMIN);
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

    /**
     * Ascender o bajar de rango (CLIENTE, PROVEEDOR, ADMIN) a un usuario ya registrado.
     * - Solo el administrador principal puede hacerlo; los usuarios ascendidos a ADMIN no.
     * - Nadie puede cambiar el rol del administrador principal.
     * - Un proveedor con herramientas no puede pasar a CLIENTE (sus herramientas quedarían sin quien las gestione);
     *   sí puede ascender a ADMIN, que gestiona todas las herramientas.
     * El usuario debe volver a iniciar sesión para ver el menú de su nuevo rol; la API aplica el rol de inmediato,
     * porque el filtro JWT lo lee de la base de datos en cada petición.
     */
    @Override
    public Usuario cambiarRol(Solicitante solicitante, Long id, String rol) {
        Usuario quienPide = usuarioRepositoryPort.buscarPorId(solicitante.id())
                .orElseThrow(() -> new AccesoDenegadoException("No se pudo identificar al usuario autenticado."));
        if (!esAdminPrincipal(quienPide)) {
            throw new AccesoDenegadoException("Solo el administrador principal puede cambiar roles.");
        }
        Usuario usuario = usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        if (esAdminPrincipal(usuario)) {
            throw new ConflictoException("El rol del administrador principal no se puede cambiar.");
        }
        Rol nuevoRol = Rol.desde(rol);
        if (usuario.getRol() == nuevoRol) {
            throw new DomainException("El usuario ya tiene el rol " + nuevoRol + ".");
        }
        if (usuario.getRol() == Rol.PROVEEDOR && nuevoRol == Rol.CLIENTE
                && !herramientaRepositoryPort.listarPorProveedor(id).isEmpty()) {
            throw new ConflictoException("El proveedor tiene herramientas registradas y no puede pasar a CLIENTE. "
                    + "Elimine sus herramientas o asígnele otro rol.");
        }
        usuario.cambiarRol(nuevoRol);
        return usuarioRepositoryPort.guardar(usuario);
    }

    private boolean esAdminPrincipal(Usuario usuario) {
        return usuario.getRol() == Rol.ADMIN && usuario.getEmail().equalsIgnoreCase(emailAdminPrincipal);
    }

    private void exigirClienteOProveedor(Usuario usuario) {
        if (usuario.getRol() != Rol.CLIENTE && usuario.getRol() != Rol.PROVEEDOR) {
            throw new DomainException("Solo se pueden gestionar clientes y proveedores.");
        }
    }
}
