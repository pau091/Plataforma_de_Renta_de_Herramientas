package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.port.out.ListaNegraTokensPort;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static com.example.tomcat.service.Datos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Cambio de rol: solo el administrador principal asciende o baja de rango")
class CambiarRolTest {

    private static final String PRINCIPAL = "admin@rentas.com";
    private static final Long OTRO_ADMIN_ID = 50L;

    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    PasswordHasherPort hasher;
    @Mock
    ListaNegraTokensPort listaNegra;

    GestionarUsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new GestionarUsuarioService(usuarios, herramientas, reservas, hasher, listaNegra, PRINCIPAL);
        existe(new Usuario(ADMIN_ID, "Administrador General", PRINCIPAL, "hash", true, null, Rol.ADMIN));
        existe(new Usuario(OTRO_ADMIN_ID, "Admin Ascendido", "ascendido@correo.com", "hash", true, null, Rol.ADMIN));
        existe(usuario(CLIENTE_ID, "Juan", Rol.CLIENTE, true));
        existe(usuario(PROVEEDOR_ID, "Carlos", Rol.PROVEEDOR, true));
        when(usuarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void existe(Usuario usuario) {
        when(usuarios.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("El admin principal asciende: CLIENTE → PROVEEDOR → ADMIN")
    void asciende() {
        assertThat(servicio.cambiarRol(ADMIN, CLIENTE_ID, "proveedor").getRol()).isEqualTo(Rol.PROVEEDOR);
        assertThat(servicio.cambiarRol(ADMIN, CLIENTE_ID, "ADMIN").getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    @DisplayName("Puede bajar de rango a otro admin")
    void bajaDeRango() {
        assertThat(servicio.cambiarRol(ADMIN, OTRO_ADMIN_ID, "CLIENTE").getRol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("Un admin ascendido (o cualquier otro rol) no puede cambiar roles")
    void soloElPrincipal() {
        Solicitante ascendido = new Solicitante(OTRO_ADMIN_ID, Rol.ADMIN);
        assertThatThrownBy(() -> servicio.cambiarRol(ascendido, CLIENTE_ID, "PROVEEDOR"))
                .isInstanceOf(AccesoDenegadoException.class).hasMessageContaining("administrador principal");
        assertThatThrownBy(() -> servicio.cambiarRol(PROVEEDOR, CLIENTE_ID, "ADMIN"))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Nadie cambia el rol del admin principal, ni él mismo")
    void principalProtegido() {
        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN, ADMIN_ID, "CLIENTE"))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("administrador principal");
    }

    @Test
    @DisplayName("Un proveedor con herramientas no pasa a CLIENTE, pero sí puede ascender a ADMIN")
    void proveedorConHerramientas() {
        when(herramientas.listarPorProveedor(PROVEEDOR_ID)).thenReturn(List.of(herramienta("DISPONIBLE")));
        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN, PROVEEDOR_ID, "CLIENTE"))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("herramientas registradas");
        assertThat(servicio.cambiarRol(ADMIN, PROVEEDOR_ID, "ADMIN").getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    @DisplayName("Valida el rol pedido, que el usuario exista y que el rol sea distinto al actual")
    void validaciones() {
        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN, CLIENTE_ID, "JEFE")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN, CLIENTE_ID, "CLIENTE"))
                .isInstanceOf(DomainException.class).hasMessageContaining("ya tiene el rol");
        when(usuarios.buscarPorId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN, 999L, "ADMIN")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
