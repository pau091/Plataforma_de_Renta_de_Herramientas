package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Factura;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("Servicios de pagos: pago con confirmación automática, corrección y reembolso")
class PagosServiceTest {

    /** 3 días × 45.000 */
    private static final BigDecimal MONTO = new BigDecimal("135000");

    @Mock
    PagoRepositoryPort pagos;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    FacturaRepositoryPort facturas;

    ProcesarPagoService procesar;
    GestionarPagoService gestionar;

    @BeforeEach
    void preparar() {
        procesar = new ProcesarPagoService(pagos, reservas, herramientas, usuarios, facturas, POLITICA);
        gestionar = new GestionarPagoService(pagos, reservas, facturas);
        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("DISPONIBLE")));
        when(usuarios.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(usuario(CLIENTE_ID, "Juan Cliente", Rol.CLIENTE, true)));
        when(pagos.guardar(any())).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            return new Pago(p.getId() != null ? p.getId() : 21L, p.getReservaId(), p.getMonto(), p.getMetodoPago(),
                    p.getEstadoPago(), p.getFechaPago());
        });
        when(reservas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Reserva existe(Reserva reserva) {
        when(reservas.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservas.bloquearPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        return reserva;
    }

    private static Factura factura(Long pagoId) {
        return new Factura(5L, "FAC-000021", pagoId, 7L, CLIENTE_ID, "Juan Cliente", "juan@correo.com", HERRAMIENTA_ID,
                "Taladro", LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 24), 3, new BigDecimal("45000"), MONTO, MONTO,
                "TARJETA", null, null);
    }

    @Nested
    @DisplayName("Pagar una reserva")
    class Pagar {

        @Test
        @DisplayName("Registra el pago, confirma la reserva y genera la factura FAC-%06d en una sola operación")
        void pagoCompleto() {
            Reserva reserva = existe(reserva(7L, "ACEPTADA"));
            ArgumentCaptor<Factura> factura = ArgumentCaptor.forClass(Factura.class);

            Pago pago = procesar.procesar(CLIENTE, 7L, MONTO, "tarjeta");

            assertThat(pago.getEstadoPago()).isEqualTo("COMPLETADO");
            assertThat(pago.getMetodoPago()).isEqualTo("TARJETA");
            assertThat(reserva.getEstado()).isEqualTo("CONFIRMADA");
            verify(reservas).guardar(reserva);
            verify(facturas).guardar(factura.capture());
            Factura f = factura.getValue();
            assertThat(f.getNumero()).isEqualTo("FAC-000021");
            assertThat(f.getPagoId()).isEqualTo(21L);
            assertThat(f.getDias()).isEqualTo(3);
            assertThat(f.getTotal()).isEqualByComparingTo(MONTO);
            assertThat(f.getClienteNombre()).isEqualTo("Juan Cliente");
            assertThat(f.getEstado()).isEqualTo("VIGENTE");
        }

        @Test
        @DisplayName("El monto debe ser exactamente tarifa × días")
        void montoExacto() {
            existe(reserva(7L, "ACEPTADA"));
            assertThatThrownBy(() -> procesar.procesar(CLIENTE, 7L, new BigDecimal("100000"), "TARJETA"))
                    .isInstanceOf(DomainException.class).hasMessageContaining("135000");
            verify(pagos, never()).guardar(any());
        }

        @Test
        @DisplayName("Solo el cliente de la reserva (o un admin) puede pagarla")
        void soloElCliente() {
            existe(reserva(7L, "ACEPTADA"));
            assertThatThrownBy(() -> procesar.procesar(OTRO_CLIENTE, 7L, MONTO, "TARJETA")).isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> procesar.procesar(PROVEEDOR, 7L, MONTO, "TARJETA")).isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("No se paga dos veces, ni sin aceptación, ni fuera de plazo, ni con método inválido")
        void casosRechazados() {
            existe(reserva(7L, "ACEPTADA"));
            when(pagos.existePagoCompletado(7L)).thenReturn(true);
            assertThatThrownBy(() -> procesar.procesar(CLIENTE, 7L, MONTO, "TARJETA")).isInstanceOf(ConflictoException.class);

            existe(reserva(8L, "PENDIENTE"));
            assertThatThrownBy(() -> procesar.procesar(CLIENTE, 8L, MONTO, "TARJETA"))
                    .isInstanceOf(DomainException.class).hasMessageContaining("aceptada por el proveedor");

            existe(new Reserva(9L, CLIENTE_ID, HERRAMIENTA_ID, HOY.plusDays(5), HOY.plusDays(7), "ACEPTADA",
                    null, null, null, null, AHORA_UTC.minusHours(3), AHORA_UTC.minusMinutes(61)));
            assertThatThrownBy(() -> procesar.procesar(CLIENTE, 9L, MONTO, "TARJETA"))
                    .isInstanceOf(ConflictoException.class).hasMessageContaining("venció");

            existe(reserva(10L, "ACEPTADA"));
            assertThatThrownBy(() -> procesar.procesar(CLIENTE, 10L, MONTO, "BITCOIN")).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Corregir y reembolsar (solo ADMIN)")
    class CorregirYReembolsar {

        @Test
        @DisplayName("Corregir el método actualiza también la factura")
        void corrigeMetodo() {
            when(pagos.buscarPorId(21L)).thenReturn(Optional.of(pago(21L, 7L, "135000", "COMPLETADO")));
            Factura f = factura(21L);
            when(facturas.buscarPorPagoId(21L)).thenReturn(Optional.of(f));

            assertThat(gestionar.actualizarMetodo(ADMIN, 21L, "efectivo").getMetodoPago()).isEqualTo("EFECTIVO");
            assertThat(f.getMetodoPago()).isEqualTo("EFECTIVO");
            verify(facturas).guardar(f);
        }

        @Test
        @DisplayName("Reembolsar: pago REEMBOLSADO, reserva CANCELADA y factura ANULADA")
        void reembolso() {
            when(pagos.buscarPorId(21L)).thenReturn(Optional.of(pago(21L, 7L, "135000", "COMPLETADO")));
            Reserva reserva = existe(reserva(7L, "CONFIRMADA"));
            Factura f = factura(21L);
            when(facturas.buscarPorPagoId(21L)).thenReturn(Optional.of(f));

            assertThat(gestionar.reembolsar(ADMIN, 21L).getEstadoPago()).isEqualTo("REEMBOLSADO");
            assertThat(reserva.getEstado()).isEqualTo("CANCELADA");
            assertThat(f.estaAnulada()).isTrue();
        }

        @Test
        @DisplayName("No se reembolsa una herramienta ya entregada")
        void noReembolsaEntregadas() {
            when(pagos.buscarPorId(21L)).thenReturn(Optional.of(pago(21L, 7L, "135000", "COMPLETADO")));
            existe(reserva(7L, "ENTREGADA"));
            assertThatThrownBy(() -> gestionar.reembolsar(ADMIN, 21L))
                    .isInstanceOf(ConflictoException.class).hasMessageContaining("ya fue entregada");
            verify(pagos, never()).guardar(any());
        }

        @Test
        @DisplayName("Ni el cliente ni el proveedor pueden corregir o reembolsar")
        void soloAdmin() {
            assertThatThrownBy(() -> gestionar.reembolsar(CLIENTE, 21L)).isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> gestionar.actualizarMetodo(PROVEEDOR, 21L, "EFECTIVO")).isInstanceOf(AccesoDenegadoException.class);
        }
    }
}
