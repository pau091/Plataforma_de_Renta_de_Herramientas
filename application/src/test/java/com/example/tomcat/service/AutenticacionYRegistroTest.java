package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.AuthenticationFailedException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.UsuarioInactivoException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.tomcat.service.Datos.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Autenticación y registro de usuarios")
class AutenticacionYRegistroTest {

    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    PasswordHasherPort hasher;

    AutenticarUsuarioService autenticar;
    RegistrarUsuarioService registrar;

    @BeforeEach
    void preparar() {
        autenticar = new AutenticarUsuarioService(usuarios, hasher);
        registrar = new RegistrarUsuarioService(usuarios, hasher);
    }

    @Test
    @DisplayName("Login correcto devuelve el usuario")
    void loginCorrecto() {
        Usuario juan = usuario(12L, "Juan Cliente", Rol.CLIENTE, true);
        when(usuarios.buscarPorEmail("juan.cliente@correo.com")).thenReturn(Optional.of(juan));
        when(hasher.matches("Rentas2026*", "hash")).thenReturn(true);

        assertThat(autenticar.autenticar(" juan.cliente@correo.com ", "Rentas2026*")).isSameAs(juan);
    }

    @Test
    @DisplayName("Correo inexistente o clave incorrecta dan el mismo error (no revela qué falló)")
    void credencialesInvalidas() {
        when(usuarios.buscarPorEmail("nadie@correo.com")).thenReturn(Optional.empty());
        when(usuarios.buscarPorEmail("juan.cliente@correo.com"))
                .thenReturn(Optional.of(usuario(12L, "Juan Cliente", Rol.CLIENTE, true)));
        when(hasher.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> autenticar.autenticar("nadie@correo.com", "x"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Credenciales inválidas.");
        assertThatThrownBy(() -> autenticar.autenticar("juan.cliente@correo.com", "mala"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Credenciales inválidas.");
    }

    @Test
    @DisplayName("Un usuario inactivo con la clave correcta recibe 'usuario inactivo'")
    void usuarioInactivo() {
        when(usuarios.buscarPorEmail("gabriel@correo.com")).thenReturn(Optional.of(usuario(41L, "Gabriel", Rol.CLIENTE, false)));
        when(hasher.matches("Rentas2026*", "hash")).thenReturn(true);

        assertThatThrownBy(() -> autenticar.autenticar("gabriel@correo.com", "Rentas2026*"))
                .isInstanceOf(UsuarioInactivoException.class);
    }

    @Test
    @DisplayName("Un usuario inactivo con clave incorrecta no descubre que existe")
    void inactivoConClaveIncorrecta() {
        when(usuarios.buscarPorEmail("gabriel@correo.com")).thenReturn(Optional.of(usuario(41L, "Gabriel", Rol.CLIENTE, false)));
        when(hasher.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> autenticar.autenticar("gabriel@correo.com", "mala"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("El registro público crea un CLIENTE activo con la clave cifrada")
    void registroPublico() {
        when(usuarios.buscarPorEmail("laura@correo.com")).thenReturn(Optional.empty());
        when(hasher.hash("Rentas2026*")).thenReturn("bcrypt");
        when(usuarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = registrar.registrarCliente("  Laura Gómez ", "laura@correo.com", "Rentas2026*", null);

        assertThat(creado.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getNombre()).isEqualTo("Laura Gómez");
        assertThat(creado.getPassword()).isEqualTo("bcrypt");
    }

    @Test
    @DisplayName("El registro público no permite pedir el rol PROVEEDOR")
    void registroPublicoNoCreaProveedores() {
        assertThatThrownBy(() -> registrar.registrarCliente("Falso", "falso@correo.com", "Rentas2026*", "PROVEEDOR"))
                .isInstanceOf(AccesoDenegadoException.class).hasMessageContaining("solo permite cuentas de CLIENTE");
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("El admin registra proveedores, pero nunca otro admin")
    void registroPorAdmin() {
        when(usuarios.buscarPorEmail("prov@correo.com")).thenReturn(Optional.empty());
        when(usuarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        registrar.registrar("Proveedor Nuevo", "prov@correo.com", "Rentas2026*", "proveedor");
        verify(usuarios).guardar(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.PROVEEDOR);

        assertThatThrownBy(() -> registrar.registrar("Otro Admin", "admin2@correo.com", "Rentas2026*", "ADMIN"))
                .isInstanceOf(DomainException.class).hasMessageContaining("administrador");
    }

    @Test
    @DisplayName("Correo repetido → conflicto; clave corta o rol inválido → error de datos")
    void validacionesDelRegistro() {
        when(usuarios.buscarPorEmail("juan.cliente@correo.com"))
                .thenReturn(Optional.of(usuario(12L, "Juan Cliente", Rol.CLIENTE, true)));

        assertThatThrownBy(() -> registrar.registrarCliente("Juan", "juan.cliente@correo.com", "Rentas2026*", null))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> registrar.registrarCliente("Juan", "nuevo@correo.com", "corta", null))
                .isInstanceOf(DomainException.class).hasMessageContaining("8 caracteres");
        assertThatThrownBy(() -> registrar.registrar("X", "x@correo.com", "Rentas2026*", "JEFE"))
                .isInstanceOf(DomainException.class);
    }
}
