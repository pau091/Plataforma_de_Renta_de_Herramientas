package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.EntradaListaEspera;
import com.example.tomcat.model.Notificacion;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ListaEsperaRepositoryPort;
import com.example.tomcat.repository.NotificacionRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Lista de espera: anotarse y aviso al primero de la fila cuando se liberan las fechas")
class ListaEsperaTest {

    @Mock
    ListaEsperaRepositoryPort listaEspera;
    @Mock
    NotificacionRepositoryPort notificaciones;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    HerramientaRepositoryPort herramientas;

    ListaEsperaService servicio;
    AvisoListaEspera aviso;

    @BeforeEach
    void preparar() {
        servicio = new ListaEsperaService(listaEspera, herramientas, reservas, POLITICA);
        aviso = new AvisoListaEspera(listaEspera, notificaciones, reservas, herramientas, POLITICA);
        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("DISPONIBLE")));
        when(listaEspera.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static EntradaListaEspera esperando(Long id, Long clienteId, int desde, int hasta) {
        return new EntradaListaEspera(id, HERRAMIENTA_ID, clienteId, HOY.plusDays(desde), HOY.plusDays(hasta), "ESPERANDO",
                AHORA_UTC.minusHours(10 - id), null);
    }

    private static Reserva cancelada(int desde, int hasta) {
        return new Reserva(50L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(desde), HOY.plusDays(hasta), "CANCELADA",
                null, null, null, null, AHORA_UTC.minusDays(1), null);
    }

    @Test
    @DisplayName("Se anota si las fechas están ocupadas; si están libres, debe reservar directamente")
    void anotarse() {
        when(reservas.existeSolape(HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(12), null)).thenReturn(true);
        assertThat(servicio.anotarse(CLIENTE, HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(12)).getEstado()).isEqualTo("ESPERANDO");

        assertThatThrownBy(() -> servicio.anotarse(CLIENTE, HERRAMIENTA_ID, HOY.plusDays(20), HOY.plusDays(21)))
                .isInstanceOf(DomainException.class).hasMessageContaining("reservarla directamente");
    }

    @Test
    @DisplayName("No se anota dos veces para las mismas fechas, ni en sus propias herramientas, ni como admin")
    void reglas() {
        when(reservas.existeSolape(eq(HERRAMIENTA_ID), any(), any(), eq(null))).thenReturn(true);
        when(listaEspera.listarPorCliente(CLIENTE_ID)).thenReturn(List.of(esperando(1L, CLIENTE_ID, 10, 12)));
        assertThatThrownBy(() -> servicio.anotarse(CLIENTE, HERRAMIENTA_ID, HOY.plusDays(11), HOY.plusDays(13)))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> servicio.anotarse(PROVEEDOR, HERRAMIENTA_ID, HOY.plusDays(11), HOY.plusDays(13)))
                .isInstanceOf(DomainException.class).hasMessageContaining("propias");
        assertThatThrownBy(() -> servicio.anotarse(ADMIN, HERRAMIENTA_ID, HOY.plusDays(11), HOY.plusDays(13)))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Al liberarse las fechas se notifica solo al primero de la fila cuyas fechas quedaron libres")
    void avisaAlPrimero() {
        EntradaListaEspera primero = esperando(1L, CLIENTE_ID, 10, 12);
        EntradaListaEspera segundo = esperando(2L, OTRO_CLIENTE_ID, 11, 12);
        when(listaEspera.listarActivasPorHerramienta(HERRAMIENTA_ID)).thenReturn(List.of(primero, segundo));

        Optional<EntradaListaEspera> notificada = aviso.fechasLiberadas(cancelada(10, 12));

        assertThat(notificada).containsSame(primero);
        assertThat(primero.getEstado()).isEqualTo("NOTIFICADA");
        assertThat(segundo.getEstado()).isEqualTo("ESPERANDO");
        ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).guardar(aviso.capture());
        assertThat(aviso.getValue().getUsuarioId()).isEqualTo(CLIENTE_ID);
        assertThat(aviso.getValue().getMensaje()).contains("Taladro Percutor").contains("27/10/2026");
    }

    @Test
    @DisplayName("Si las fechas del primero siguen ocupadas por otra reserva, se avisa al siguiente que sí quedó libre")
    void saltaAQuienSigueOcupado() {
        EntradaListaEspera primero = esperando(1L, CLIENTE_ID, 10, 14);    // el 13-14 siguen ocupados
        EntradaListaEspera segundo = esperando(2L, OTRO_CLIENTE_ID, 10, 11);
        when(listaEspera.listarActivasPorHerramienta(HERRAMIENTA_ID)).thenReturn(List.of(primero, segundo));
        when(reservas.existeSolape(HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(14), null)).thenReturn(true);

        assertThat(aviso.fechasLiberadas(cancelada(10, 12))).containsSame(segundo);
        assertThat(primero.getEstado()).isEqualTo("ESPERANDO");
    }

    @Test
    @DisplayName("Nadie esperando esas fechas, o la herramienta no está disponible: no se notifica a nadie")
    void sinAviso() {
        when(listaEspera.listarActivasPorHerramienta(HERRAMIENTA_ID)).thenReturn(List.of(esperando(1L, CLIENTE_ID, 20, 22)));
        assertThat(aviso.fechasLiberadas(cancelada(10, 12))).isEmpty();

        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("MANTENIMIENTO")));
        when(listaEspera.listarActivasPorHerramienta(HERRAMIENTA_ID)).thenReturn(List.of(esperando(1L, CLIENTE_ID, 10, 12)));
        assertThat(aviso.fechasLiberadas(cancelada(10, 12))).isEmpty();
        verify(notificaciones, never()).guardar(any());
    }

    @Test
    @DisplayName("Solo quien se anotó puede salir de la lista")
    void cancelar() {
        when(listaEspera.buscarPorId(1L)).thenReturn(Optional.of(esperando(1L, CLIENTE_ID, 10, 12)));
        assertThatThrownBy(() -> servicio.cancelar(OTRO_CLIENTE, 1L)).isInstanceOf(AccesoDenegadoException.class);
        assertThat(servicio.cancelar(CLIENTE, 1L).getEstado()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("Al reservar las fechas que esperaba, su entrada pasa a ATENDIDA")
    void reservarAtiende() {
        EntradaListaEspera notificada = esperando(1L, CLIENTE_ID, 10, 12);
        notificada.notificar(AHORA_UTC);
        when(listaEspera.listarPorCliente(CLIENTE_ID)).thenReturn(List.of(notificada));
        when(herramientas.bloquearPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("DISPONIBLE")));
        when(reservas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        CrearReservaService crear = new CrearReservaService(herramientas, null, reservas, POLITICA, listaEspera);

        crear.crear(CLIENTE, null, HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(12));

        assertThat(notificada.getEstado()).isEqualTo("ATENDIDA");
    }
}
