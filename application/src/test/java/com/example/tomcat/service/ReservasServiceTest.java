package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.tomcat.service.Datos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Servicios de reservas")
class ReservasServiceTest {

    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    MantenimientoRepositoryPort mantenimientos;
    @Mock
    PagoRepositoryPort pagos;

    CrearReservaService crear;
    GestionarReservaService gestionar;
    VencerReservasService vencer;
    Herramienta taladro;

    @BeforeEach
    void preparar() {
        crear = new CrearReservaService(herramientas, usuarios, reservas, POLITICA);
        gestionar = new GestionarReservaService(reservas, herramientas, mantenimientos, new ControlAcceso(herramientas), POLITICA);
        vencer = new VencerReservasService(reservas, pagos, POLITICA);
        taladro = herramienta("DISPONIBLE");
        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(taladro));
        when(herramientas.bloquearPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(taladro));
        when(reservas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void existe(Reserva reserva) {
        when(reservas.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    @Nested
    @DisplayName("Crear reserva")
    class Crear {

        @Test
        @DisplayName("El cliente reserva a su nombre aunque envíe otro clienteId; nace PENDIENTE")
        void clienteReservaASuNombre() {
            Reserva r = crear.crear(CLIENTE, 99L, HERRAMIENTA_ID, HOY.plusDays(2), HOY.plusDays(4));

            assertThat(r.getClienteId()).isEqualTo(CLIENTE_ID);
            assertThat(r.getEstado()).isEqualTo("PENDIENTE");
            assertThat(r.getFechaCreacion()).isEqualTo(AHORA_UTC);
        }

        @Test
        @DisplayName("Se puede reservar desde hoy (según la zona del negocio)")
        void reservaDesdeHoy() {
            assertThat(crear.crear(CLIENTE, null, HERRAMIENTA_ID, HOY, HOY).getFechaInicio()).isEqualTo(HOY);
        }

        @Test
        @DisplayName("Valida disponibilidad: herramienta no operativa o fechas ocupadas")
        void validaDisponibilidad() {
            when(reservas.existeSolape(HERRAMIENTA_ID, HOY.plusDays(2), HOY.plusDays(4), null)).thenReturn(true);
            assertThatThrownBy(() -> crear.crear(CLIENTE, null, HERRAMIENTA_ID, HOY.plusDays(2), HOY.plusDays(4)))
                    .isInstanceOf(ConflictoException.class).hasMessageContaining("ya tiene una reserva");

            taladro.cambiarEstado("MANTENIMIENTO");
            assertThatThrownBy(() -> crear.crear(CLIENTE, null, HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(11)))
                    .isInstanceOf(DomainException.class).hasMessageContaining("no se encuentra disponible");
            verify(reservas, never()).guardar(any());
        }

        @Test
        @DisplayName("Concurrencia: bloquea la herramienta antes de validar el cruce de fechas y de guardar")
        void bloqueaAntesDeValidarElCruce() {
            crear.crear(CLIENTE, null, HERRAMIENTA_ID, HOY.plusDays(2), HOY.plusDays(4));

            InOrder orden = inOrder(herramientas, reservas);
            orden.verify(herramientas).bloquearPorId(HERRAMIENTA_ID);
            orden.verify(reservas).existeSolape(HERRAMIENTA_ID, HOY.plusDays(2), HOY.plusDays(4), null);
            orden.verify(reservas).guardar(any());
        }

        @Test
        @DisplayName("Rechaza fechas pasadas y herramientas inexistentes")
        void rechazaDatosInvalidos() {
            assertThatThrownBy(() -> crear.crear(CLIENTE, null, HERRAMIENTA_ID, HOY.minusDays(1), HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("pasada");
            when(herramientas.bloquearPorId(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> crear.crear(CLIENTE, null, 404L, HOY, HOY)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("El admin debe indicar un cliente válido; el proveedor no puede reservar")
        void reglasPorRol() {
            assertThatThrownBy(() -> crear.crear(ADMIN, null, HERRAMIENTA_ID, HOY, HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("clienteId");
            when(usuarios.buscarPorId(PROVEEDOR_ID)).thenReturn(Optional.of(usuario(PROVEEDOR_ID, "Carlos", Rol.PROVEEDOR, true)));
            assertThatThrownBy(() -> crear.crear(ADMIN, PROVEEDOR_ID, HERRAMIENTA_ID, HOY, HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("no es un cliente");
            assertThatThrownBy(() -> crear.crear(PROVEEDOR, CLIENTE_ID, HERRAMIENTA_ID, HOY, HOY))
                    .isInstanceOf(AccesoDenegadoException.class);

            when(usuarios.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(usuario(CLIENTE_ID, "Juan", Rol.CLIENTE, true)));
            assertThat(crear.crear(ADMIN, CLIENTE_ID, HERRAMIENTA_ID, HOY, HOY).getClienteId()).isEqualTo(CLIENTE_ID);
        }
    }

    @Nested
    @DisplayName("Aceptar, rechazar, entregar y devolver")
    class Gestionar {

        @Test
        @DisplayName("El proveedor dueño acepta y queda registrada la hora de aceptación (UTC)")
        void aceptar() {
            Reserva r = reserva(1L, "PENDIENTE");
            existe(r);
            assertThat(gestionar.aceptar(PROVEEDOR, 1L).getEstado()).isEqualTo("ACEPTADA");
            assertThat(r.getFechaAceptacion()).isEqualTo(AHORA_UTC);
        }

        @Test
        @DisplayName("Otro proveedor o el cliente no pueden aceptar")
        void soloElDuenoAcepta() {
            existe(reserva(1L, "PENDIENTE"));
            assertThatThrownBy(() -> gestionar.aceptar(OTRO_PROVEEDOR, 1L)).isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> gestionar.aceptar(CLIENTE, 1L)).isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("No se acepta pasada la hora de plazo, aunque el vencimiento automático no haya corrido")
        void noAceptaVencidas() {
            existe(new Reserva(1L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(5), HOY.plusDays(6), "PENDIENTE",
                    null, null, null, null, AHORA_UTC.minusMinutes(90), null));
            assertThatThrownBy(() -> gestionar.aceptar(PROVEEDOR, 1L))
                    .isInstanceOf(ConflictoException.class).hasMessageContaining("venció");
        }

        @Test
        @DisplayName("No se acepta si las fechas ya se ocuparon o la herramienta no está operativa")
        void noAceptaSinDisponibilidad() {
            existe(reserva(1L, "PENDIENTE"));
            when(reservas.existeSolape(eq(HERRAMIENTA_ID), any(), any(), eq(1L))).thenReturn(true);
            assertThatThrownBy(() -> gestionar.aceptar(PROVEEDOR, 1L)).isInstanceOf(ConflictoException.class);

            when(reservas.existeSolape(eq(HERRAMIENTA_ID), any(), any(), eq(1L))).thenReturn(false);
            taladro.cambiarEstado("FUERA_DE_SERVICIO");
            assertThatThrownBy(() -> gestionar.aceptar(PROVEEDOR, 1L)).isInstanceOf(DomainException.class);
        }

        @Test
        void rechazarYEntregar() {
            existe(reserva(1L, "PENDIENTE"));
            existe(reserva(2L, "CONFIRMADA"));
            assertThat(gestionar.rechazar(PROVEEDOR, 1L).getEstado()).isEqualTo("RECHAZADA");
            assertThat(gestionar.entregar(ADMIN, 2L).getEstado()).isEqualTo("ENTREGADA");
        }

        @Test
        @DisplayName("Devolución con daño: herramienta a MANTENIMIENTO y se abre un mantenimiento ligado a la reserva")
        void devolucionConDano() {
            existe(reserva(1L, "ENTREGADA"));
            ArgumentCaptor<Mantenimiento> mantenimiento = ArgumentCaptor.forClass(Mantenimiento.class);

            Reserva r = gestionar.devolver(PROVEEDOR, 1L, "DAÑADO", "Cable pelado");

            assertThat(r.getEstado()).isEqualTo("FINALIZADA");
            assertThat(taladro.getEstado()).isEqualTo("MANTENIMIENTO");
            verify(herramientas).guardar(taladro);
            verify(mantenimientos).guardar(mantenimiento.capture());
            assertThat(mantenimiento.getValue().getReservaId()).isEqualTo(1L);
            assertThat(mantenimiento.getValue().getDescripcionProblema()).contains("reserva 1").contains("Cable pelado");
            assertThat(mantenimiento.getValue().estaAbierto()).isTrue();
        }

        @Test
        @DisplayName("Devolución con pérdida: herramienta FUERA_DE_SERVICIO, sin mantenimiento")
        void devolucionConPerdida() {
            existe(reserva(1L, "ENTREGADA"));
            gestionar.devolver(PROVEEDOR, 1L, "PERDIDO", null);
            assertThat(taladro.getEstado()).isEqualTo("FUERA_DE_SERVICIO");
            verify(mantenimientos, never()).guardar(any());
        }

        @Test
        @DisplayName("Devolución en buen estado: la herramienta sigue igual")
        void devolucionEnBuenEstado() {
            existe(reserva(1L, "ENTREGADA"));
            gestionar.devolver(PROVEEDOR, 1L, "BUENO", null);
            assertThat(taladro.getEstado()).isEqualTo("DISPONIBLE");
            verify(herramientas, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("Modificar y cancelar (cliente)")
    class ModificarYCancelar {

        @Test
        @DisplayName("El cliente cambia las fechas de su reserva pendiente")
        void modificaFechas() {
            existe(reserva(1L, "PENDIENTE"));
            Reserva r = gestionar.modificarFechas(CLIENTE, 1L, HOY.plusDays(10), HOY.plusDays(12));
            assertThat(r.getFechaInicio()).isEqualTo(HOY.plusDays(10));
        }

        @Test
        @DisplayName("Las nuevas fechas no pueden cruzarse con otra reserva (sin contarse a sí misma)")
        void modificaSinCruces() {
            existe(reserva(1L, "PENDIENTE"));
            when(reservas.existeSolape(HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(12), 1L)).thenReturn(true);
            assertThatThrownBy(() -> gestionar.modificarFechas(CLIENTE, 1L, HOY.plusDays(10), HOY.plusDays(12)))
                    .isInstanceOf(ConflictoException.class);

            InOrder orden = inOrder(herramientas, reservas);
            orden.verify(herramientas).bloquearPorId(HERRAMIENTA_ID);
            orden.verify(reservas).existeSolape(HERRAMIENTA_ID, HOY.plusDays(10), HOY.plusDays(12), 1L);
        }

        @Test
        @DisplayName("Solo su cliente (o un admin) puede modificarla o cancelarla")
        void soloElTitular() {
            existe(reserva(1L, "PENDIENTE"));
            assertThatThrownBy(() -> gestionar.cancelar(OTRO_CLIENTE, 1L)).isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> gestionar.modificarFechas(PROVEEDOR, 1L, HOY.plusDays(9), HOY.plusDays(9)))
                    .isInstanceOf(AccesoDenegadoException.class);
            assertThat(gestionar.cancelar(CLIENTE, 1L).getEstado()).isEqualTo("CANCELADA");
        }

        @Test
        @DisplayName("No se modifica una reserva cuyo plazo ya venció")
        void noModificaVencidas() {
            existe(new Reserva(1L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(5), HOY.plusDays(6), "PENDIENTE",
                    null, null, null, null, AHORA_UTC.minusHours(2), null));
            assertThatThrownBy(() -> gestionar.modificarFechas(CLIENTE, 1L, HOY.plusDays(8), HOY.plusDays(9)))
                    .isInstanceOf(ConflictoException.class);
        }
    }

    @Nested
    @DisplayName("Listado según el rol")
    class Listar {

        @Test
        @DisplayName("El cliente solo ve las suyas y no puede pedir las de otro")
        void clienteVeLasSuyas() {
            when(reservas.listarPorCliente(CLIENTE_ID)).thenReturn(List.of(reserva(1L, "PENDIENTE"), reserva(2L, "FINALIZADA")));
            assertThat(gestionar.listar(CLIENTE, null, null, null, null)).hasSize(2);
            assertThat(gestionar.listar(CLIENTE, null, null, null, "finalizada")).extracting(Reserva::getId).containsExactly(2L);
            assertThatThrownBy(() -> gestionar.listar(CLIENTE, OTRO_CLIENTE_ID, null, null, null))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("El proveedor ve las de sus herramientas; un estado desconocido es un error")
        void proveedorVeLasDeSusHerramientas() {
            when(reservas.listarPorProveedor(PROVEEDOR_ID)).thenReturn(List.of(reserva(1L, "PENDIENTE")));
            assertThat(gestionar.listar(PROVEEDOR, null, null, null, "PENDIENTE")).hasSize(1);
            assertThatThrownBy(() -> gestionar.listar(PROVEEDOR, null, OTRO_PROVEEDOR_ID, null, null))
                    .isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> gestionar.listar(PROVEEDOR, null, null, null, "ROTA"))
                    .isInstanceOf(DomainException.class).hasMessageContaining("Estado de reserva no válido");
        }
    }

    @Nested
    @DisplayName("Vencimiento automático")
    class Vencer {

        @Test
        @DisplayName("Vence las PENDIENTE/ACEPTADA fuera de plazo, salvo las que ya tienen un pago completado")
        void venceSoloLasQueCorresponde() {
            LocalDateTime haceDosHoras = AHORA_UTC.minusHours(2);
            Reserva vieja = new Reserva(1L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(3), HOY.plusDays(4), "PENDIENTE",
                    null, null, null, null, haceDosHoras, null);
            Reserva viejaPagada = new Reserva(2L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(5), HOY.plusDays(6), "ACEPTADA",
                    null, null, null, null, haceDosHoras, haceDosHoras);
            Reserva reciente = reserva(3L, "PENDIENTE");
            Reserva inicioPasado = new Reserva(4L, CLIENTE_ID, HERRAMIENTA_ID, LocalDate.of(2026, 10, 16), HOY, "ACEPTADA",
                    null, null, null, null, AHORA_UTC.minusMinutes(5), AHORA_UTC.minusMinutes(1));
            when(reservas.listarPorEstados(List.of("PENDIENTE", "ACEPTADA"))).thenReturn(List.of(vieja, viejaPagada, reciente, inicioPasado));
            when(pagos.existePagoCompletado(anyLong())).thenReturn(false);
            when(pagos.existePagoCompletado(2L)).thenReturn(true);

            List<Reserva> vencidas = vencer.vencerReservas();

            assertThat(vencidas).extracting(Reserva::getId).containsExactly(1L, 4L);
            assertThat(vencidas).allMatch(r -> "VENCIDA".equals(r.getEstado()));
            assertThat(reciente.getEstado()).isEqualTo("PENDIENTE");
            assertThat(viejaPagada.getEstado()).isEqualTo("ACEPTADA");
        }
    }
}
