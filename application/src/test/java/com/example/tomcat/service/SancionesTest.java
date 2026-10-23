package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.dto.Sancion;
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

import java.util.Optional;

import static com.example.tomcat.service.Datos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Suspensión y baneo con expulsión inmediata (lista negra de tokens)")
class SancionesTest {

    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    ListaNegraTokensPort listaNegra;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    PasswordHasherPort hasher;

    SancionarUsuarioService sancionar;
    Usuario juan;

    @BeforeEach
    void preparar() {
        sancionar = new SancionarUsuarioService(usuarios, listaNegra, RELOJ);
        juan = usuario(CLIENTE_ID, "Juan", Rol.CLIENTE, true);
        when(usuarios.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(juan));
        when(usuarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listaNegra.revocarTodos(anyLong(), anyString())).thenReturn(2);
    }

    @Test
    @DisplayName("Suspender bloquea la cuenta por N días y revoca al instante todos sus tokens")
    void suspender() {
        Sancion sancion = sancionar.suspender(CLIENTE_ID, 7, "Cancelaciones reiteradas");

        assertThat(sancion.tokensRevocados()).isEqualTo(2);
        assertThat(juan.getSuspendidoHasta()).isEqualTo(AHORA_UTC.plusDays(7));
        assertThat(juan.estadoCuenta(AHORA_UTC)).isEqualTo("SUSPENDIDA");
        assertThat(juan.estadoCuenta(AHORA_UTC.plusDays(8))).isEqualTo("ACTIVA");   // se libera sola
        verify(listaNegra).revocarTodos(eq(CLIENTE_ID), contains("Cancelaciones reiteradas"));
    }

    @Test
    @DisplayName("Banear es indefinido y también revoca sus tokens; reactivar lo libera")
    void banearYReactivar() {
        Sancion sancion = sancionar.banear(CLIENTE_ID, "Fraude en pagos");
        assertThat(sancion.usuario().estadoCuenta(AHORA_UTC.plusYears(5))).isEqualTo("BANEADA");
        verify(listaNegra).revocarTodos(eq(CLIENTE_ID), contains("baneada"));

        assertThatThrownBy(() -> sancionar.banear(CLIENTE_ID, "Otra vez")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> sancionar.suspender(CLIENTE_ID, 3, "x")).isInstanceOf(DomainException.class)
                .hasMessageContaining("baneado");

        assertThat(sancionar.reactivar(CLIENTE_ID).estadoCuenta(AHORA_UTC)).isEqualTo("ACTIVA");
        assertThatThrownBy(() -> sancionar.reactivar(CLIENTE_ID)).isInstanceOf(DomainException.class)
                .hasMessageContaining("no tiene una suspensión");
    }

    @Test
    @DisplayName("Valida días (1 a 365), motivo, que exista y que no sea un administrador")
    void validaciones() {
        assertThatThrownBy(() -> sancionar.suspender(CLIENTE_ID, 0, "Motivo")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> sancionar.suspender(CLIENTE_ID, 366, "Motivo")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> sancionar.suspender(CLIENTE_ID, 3, "  ")).isInstanceOf(DomainException.class)
                .hasMessageContaining("motivo");

        when(usuarios.buscarPorId(ADMIN_ID)).thenReturn(Optional.of(usuario(ADMIN_ID, "Admin", Rol.ADMIN, true)));
        assertThatThrownBy(() -> sancionar.banear(ADMIN_ID, "Motivo")).isInstanceOf(DomainException.class)
                .hasMessageContaining("proveedores y clientes");
        when(usuarios.buscarPorId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sancionar.banear(999L, "Motivo")).isInstanceOf(RecursoNoEncontradoException.class);

        verify(listaNegra, never()).revocarTodos(anyLong(), anyString());
    }

    @Test
    @DisplayName("Desactivar una cuenta (activo=false) también cierra sus sesiones con la lista negra")
    void desactivarRevocaTokens() {
        GestionarUsuarioService gestionar = new GestionarUsuarioService(usuarios, herramientas, reservas, hasher,
                listaNegra, "admin@rentas.com");

        gestionar.actualizar(CLIENTE_ID, null, null, false, null);

        assertThat(juan.isActivo()).isFalse();
        verify(listaNegra).revocarTodos(eq(CLIENTE_ID), contains("desactivada"));
    }
}
