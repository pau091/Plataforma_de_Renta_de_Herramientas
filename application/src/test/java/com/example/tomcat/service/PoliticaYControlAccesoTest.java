package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.HerramientaRepositoryPort;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PoliticaVencimiento, ControlAcceso y Solicitante")
class PoliticaYControlAccesoTest {

    @Mock
    HerramientaRepositoryPort herramientas;

    ControlAcceso control;

    @BeforeEach
    void preparar() {
        control = new ControlAcceso(herramientas);
        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("DISPONIBLE")));
    }

    @Test
    @DisplayName("\"Hoy\" usa la zona del negocio y los instantes se toman en UTC")
    void relojDelNegocio() {
        assertThat(POLITICA.hoy()).isEqualTo(HOY);
        assertThat(POLITICA.ahoraUtc()).isEqualTo(AHORA_UTC);
    }

    @Test
    @DisplayName("Una reserva pendiente vence al pasar 1 hora desde su creación")
    void politicaDeVencimiento() {
        Reserva reciente = reserva(1L, "PENDIENTE");
        Reserva vieja = new Reserva(2L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(5), HOY.plusDays(6), "PENDIENTE",
                null, null, null, null, AHORA_UTC.minusMinutes(61), null);
        assertThat(POLITICA.estaVencida(reciente)).isFalse();
        assertThat(POLITICA.estaVencida(vieja)).isTrue();
    }

    @Test
    @DisplayName("Lectura de una reserva: su cliente, el proveedor dueño o un admin")
    void accesoALaReserva() {
        Reserva r = reserva(1L, "PENDIENTE");
        assertThatCode(() -> control.verificarAccesoReserva(CLIENTE, r)).doesNotThrowAnyException();
        assertThatCode(() -> control.verificarAccesoReserva(PROVEEDOR, r)).doesNotThrowAnyException();
        assertThatCode(() -> control.verificarAccesoReserva(ADMIN, r)).doesNotThrowAnyException();
        assertThatThrownBy(() -> control.verificarAccesoReserva(OTRO_CLIENTE, r)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> control.verificarAccesoReserva(OTRO_PROVEEDOR, r)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Gestionar una reserva (aceptar, entregar...): solo el proveedor dueño o un admin")
    void gestionDeLaReserva() {
        Reserva r = reserva(1L, "PENDIENTE");
        assertThatCode(() -> control.verificarGestionReserva(PROVEEDOR, r)).doesNotThrowAnyException();
        assertThatCode(() -> control.verificarGestionReserva(ADMIN, r)).doesNotThrowAnyException();
        assertThatThrownBy(() -> control.verificarGestionReserva(CLIENTE, r)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> control.verificarGestionReserva(OTRO_PROVEEDOR, r)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Modificar o cancelar una reserva: solo su cliente o un admin")
    void titularDeLaReserva() {
        Reserva r = reserva(1L, "PENDIENTE");
        assertThatCode(() -> control.verificarTitularReserva(CLIENTE, r)).doesNotThrowAnyException();
        assertThatCode(() -> control.verificarTitularReserva(ADMIN, r)).doesNotThrowAnyException();
        assertThatThrownBy(() -> control.verificarTitularReserva(PROVEEDOR, r)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> control.verificarTitularReserva(OTRO_CLIENTE, r)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Una herramienta solo la gestiona su proveedor o un admin")
    void propietarioDeHerramienta() {
        assertThatCode(() -> control.verificarPropietarioHerramienta(PROVEEDOR, herramienta("DISPONIBLE"))).doesNotThrowAnyException();
        assertThatCode(() -> control.verificarPropietarioHerramienta(ADMIN, herramienta("DISPONIBLE"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> control.verificarPropietarioHerramienta(OTRO_PROVEEDOR, herramienta("DISPONIBLE")))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> control.verificarPropietarioHerramienta(CLIENTE, herramienta("DISPONIBLE")))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Sin id o sin rol no hay solicitante válido")
    void solicitanteIncompleto() {
        assertThatThrownBy(() -> new Solicitante(null, Rol.CLIENTE)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> new Solicitante(1L, null)).isInstanceOf(AccesoDenegadoException.class);
    }
}
