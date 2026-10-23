package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.SancionarUsuarioUseCase;
import com.example.tomcat.port.in.dto.Sancion;
import com.example.tomcat.port.out.ListaNegraTokensPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Suspensión y baneo con expulsión inmediata. Cambiar el estado de la cuenta impide nuevos inicios de sesión; además,
 * todos los tokens vigentes del usuario pasan a la lista negra, así que queda fuera de la plataforma en su siguiente
 * petición, sin esperar a que el token expire. Todo ocurre en una sola transacción.
 */
public class SancionarUsuarioService implements SancionarUsuarioUseCase {

    static final int MAX_DIAS_SUSPENSION = 365;

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ListaNegraTokensPort listaNegraTokensPort;
    private final Clock reloj;

    public SancionarUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort, ListaNegraTokensPort listaNegraTokensPort,
                                   Clock reloj) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.listaNegraTokensPort = listaNegraTokensPort;
        this.reloj = reloj;
    }

    @Override
    public Sancion suspender(Long usuarioId, int dias, String motivo) {
        if (dias < 1 || dias > MAX_DIAS_SUSPENSION) {
            throw new DomainException("La suspensión debe durar entre 1 y " + MAX_DIAS_SUSPENSION + " días.");
        }
        Usuario usuario = buscarSancionable(usuarioId);
        LocalDateTime ahora = ahoraUtc();
        usuario.suspender(ahora.plusDays(dias), motivo, ahora);
        Usuario guardado = usuarioRepositoryPort.guardar(usuario);
        int revocados = listaNegraTokensPort.revocarTodos(usuarioId, "Cuenta suspendida: " + guardado.getMotivoSancion());
        return new Sancion(guardado, revocados);
    }

    @Override
    public Sancion banear(Long usuarioId, String motivo) {
        Usuario usuario = buscarSancionable(usuarioId);
        usuario.banear(motivo);
        Usuario guardado = usuarioRepositoryPort.guardar(usuario);
        int revocados = listaNegraTokensPort.revocarTodos(usuarioId, "Cuenta baneada: " + guardado.getMotivoSancion());
        return new Sancion(guardado, revocados);
    }

    /** Los tokens revocados no se recuperan: el usuario vuelve a entrar iniciando sesión. */
    @Override
    public Usuario reactivar(Long usuarioId) {
        Usuario usuario = buscarSancionable(usuarioId);
        usuario.levantarSancion(ahoraUtc());
        return usuarioRepositoryPort.guardar(usuario);
    }

    private Usuario buscarSancionable(Long usuarioId) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        if (usuario.getRol() != Rol.CLIENTE && usuario.getRol() != Rol.PROVEEDOR) {
            throw new DomainException("Solo se pueden suspender o banear proveedores y clientes.");
        }
        return usuario;
    }

    private LocalDateTime ahoraUtc() {
        return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
    }
}
