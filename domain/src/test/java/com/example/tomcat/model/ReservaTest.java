package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Reserva: ciclo de vida y reglas de fechas")
class ReservaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 17);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 15, 0);

    private static Reserva reserva(String estado) {
        return reserva(estado, HOY.plusDays(5), HOY.plusDays(7), AHORA.minusMinutes(10), null);
    }

    private static Reserva reserva(String estado, LocalDate inicio, LocalDate fin, LocalDateTime creada, LocalDateTime aceptada) {
        return new Reserva(1L, 12L, 3L, inicio, fin, estado, null, null, null, null, creada, aceptada);
    }

    private static final BigDecimal TARIFA = new BigDecimal("45000");
    private static final BigDecimal RECARGO = new BigDecimal("0.5");
    private static final BigDecimal DEPOSITO = new BigDecimal("90000");

    /** Devolución el mismo día de la fecha fin: sin multa. */
    private static void devolverATiempo(Reserva r, String estado, String observaciones) {
        r.devolver(estado, observaciones, r.getFechaFin(), TARIFA, RECARGO);
    }

    /** Reserva entregada (del día +5 al +7) con el depósito retenido. */
    private static Reserva entregadaConDeposito(BigDecimal deposito) {
        return new Reserva(1L, 12L, 3L, HOY.plusDays(5), HOY.plusDays(7), "ENTREGADA", AHORA, null, null, null,
                AHORA.minusDays(1), AHORA.minusDays(1), Garantia.retenida(deposito));
    }

    @Test
    @DisplayName("Sin estado nace PENDIENTE")
    void naceEnPendiente() {
        assertThat(reserva(null).getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Los días de alquiler incluyen ambos extremos")
    void diasAlquilerIncluyeExtremos() {
        assertThat(reserva("PENDIENTE", HOY, HOY, AHORA, null).diasAlquiler()).isEqualTo(1);
        assertThat(reserva("PENDIENTE", HOY, HOY.plusDays(2), AHORA, null).diasAlquiler()).isEqualTo(3);
    }

    @Nested
    @DisplayName("Flujo feliz: PENDIENTE → ACEPTADA → CONFIRMADA → ENTREGADA → FINALIZADA")
    class FlujoCompleto {

        @Test
        void recorreTodosLosEstados() {
            Reserva r = reserva("PENDIENTE");

            r.aceptar(AHORA);
            assertThat(r.getEstado()).isEqualTo("ACEPTADA");
            assertThat(r.getFechaAceptacion()).isEqualTo(AHORA);

            r.confirmarPago(DEPOSITO);
            assertThat(r.getEstado()).isEqualTo("CONFIRMADA");
            assertThat(r.getGarantia().estado()).isEqualTo("RETENIDO");

            r.entregar();
            assertThat(r.getEstado()).isEqualTo("ENTREGADA");
            assertThat(r.getFechaEntrega()).isNotNull();

            devolverATiempo(r, "BUENO", "  Sin novedades  ");
            assertThat(r.getEstado()).isEqualTo("FINALIZADA");
            assertThat(r.getFechaDevolucion()).isNotNull();
            assertThat(r.getEstadoDevolucion()).isEqualTo("BUENO");
            assertThat(r.getObservacionesDevolucion()).isEqualTo("Sin novedades");
        }
    }

    @Nested
    @DisplayName("Transiciones no permitidas")
    class TransicionesInvalidas {

        @ParameterizedTest(name = "no se acepta una reserva {0}")
        @ValueSource(strings = {"ACEPTADA", "CONFIRMADA", "RECHAZADA", "VENCIDA", "CANCELADA"})
        void soloSeAceptanPendientes(String estado) {
            assertThatThrownBy(() -> reserva(estado).aceptar(AHORA))
                    .isInstanceOf(DomainException.class).hasMessageContaining("pendientes");
        }

        @Test
        @DisplayName("No se paga una reserva que el proveedor no ha aceptado")
        void noSePagaSinAceptar() {
            assertThatThrownBy(() -> reserva("PENDIENTE").confirmarPago(DEPOSITO))
                    .isInstanceOf(DomainException.class).hasMessageContaining("aceptada por el proveedor");
        }

        @Test
        @DisplayName("No se entrega una reserva sin pagar")
        void noSeEntregaSinPagar() {
            assertThatThrownBy(() -> reserva("ACEPTADA").entregar())
                    .isInstanceOf(DomainException.class).hasMessageContaining("confirmadas");
        }

        @Test
        @DisplayName("No se devuelve lo que no se ha entregado")
        void noSeDevuelveSinEntregar() {
            assertThatThrownBy(() -> devolverATiempo(reserva("CONFIRMADA"), "BUENO", null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Solo se rechazan reservas pendientes")
        void soloSeRechazanPendientes() {
            Reserva pendiente = reserva("PENDIENTE");
            pendiente.rechazar();
            assertThat(pendiente.getEstado()).isEqualTo("RECHAZADA");
            assertThatThrownBy(() -> reserva("ACEPTADA").rechazar()).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Devolución")
    class Devolucion {

        @ParameterizedTest(name = "\"{0}\" se guarda como {1}")
        @CsvSource({"BUENO,BUENO", "danado,DANADO", "DAÑADO,DANADO", "dañado,DANADO", "Perdido,PERDIDO"})
        void normalizaElEstadoDelEquipo(String recibido, String esperado) {
            Reserva r = reserva("ENTREGADA");
            devolverATiempo(r, recibido, null);
            assertThat(r.getEstadoDevolucion()).isEqualTo(esperado);
        }

        @Test
        void detectaDanoYPerdida() {
            Reserva danada = reserva("ENTREGADA");
            devolverATiempo(danada, "DANADO", "Cable roto");
            assertThat(danada.tieneDano()).isTrue();
            assertThat(danada.estaPerdida()).isFalse();

            Reserva perdida = reserva("ENTREGADA");
            devolverATiempo(perdida, "PERDIDO", "");
            assertThat(perdida.estaPerdida()).isTrue();
            assertThat(perdida.getObservacionesDevolucion()).isNull();
        }

        @ParameterizedTest(name = "estado del equipo inválido: \"{0}\"")
        @ValueSource(strings = {"ROTO", "regular", " "})
        void rechazaEstadosDesconocidos(String estado) {
            assertThatThrownBy(() -> devolverATiempo(reserva("ENTREGADA"), estado, null)).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Depósito de garantía y multa por devolución tardía (tarifa 45.000, recargo 50%, depósito 90.000)")
    class DepositoYMulta {

        @Test
        @DisplayName("A tiempo y en buen estado: se devuelve el depósito completo")
        void devueltoCompleto() {
            Reserva r = entregadaConDeposito(DEPOSITO);
            devolverATiempo(r, "BUENO", null);
            Garantia g = r.getGarantia();
            assertThat(g.estado()).isEqualTo("DEVUELTO");
            assertThat(g.montoDevuelto()).isEqualByComparingTo("90000");
            assertThat(g.diasRetraso()).isZero();
            assertThat(g.multaRetraso()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("1 día tarde: multa 67.500 (45.000 × 1,5) descontada del depósito → devuelve 22.500")
        void unDiaTarde() {
            Reserva r = entregadaConDeposito(DEPOSITO);
            r.devolver("BUENO", null, r.getFechaFin().plusDays(1), TARIFA, RECARGO);
            Garantia g = r.getGarantia();
            assertThat(g.diasRetraso()).isEqualTo(1);
            assertThat(g.multaRetraso()).isEqualByComparingTo("67500");
            assertThat(g.montoDevuelto()).isEqualByComparingTo("22500");
            assertThat(g.saldoPendiente()).isEqualByComparingTo("0");
            assertThat(g.estado()).isEqualTo("DEVUELTO_PARCIAL");
        }

        @Test
        @DisplayName("2 días tarde: la multa (135.000) supera el depósito → no se devuelve y quedan 45.000 de saldo")
        void multaMayorQueElDeposito() {
            Reserva r = entregadaConDeposito(DEPOSITO);
            r.devolver("BUENO", null, r.getFechaFin().plusDays(2), TARIFA, RECARGO);
            Garantia g = r.getGarantia();
            assertThat(g.multaRetraso()).isEqualByComparingTo("135000");
            assertThat(g.montoDevuelto()).isEqualByComparingTo("0");
            assertThat(g.saldoPendiente()).isEqualByComparingTo("45000");
            assertThat(g.estado()).isEqualTo("NO_DEVUELTO");
        }

        @Test
        @DisplayName("Dañada: queda pendiente hasta conocer el costo de reparación, que se descuenta al cerrarla")
        void danadaPendienteDeReparacion() {
            Reserva r = entregadaConDeposito(DEPOSITO);
            devolverATiempo(r, "DANADO", "Cable roto");
            assertThat(r.getGarantia().estado()).isEqualTo("PENDIENTE_REPARACION");
            assertThat(r.getGarantia().enCustodia()).isTrue();

            r.liquidarReparacion(new BigDecimal("30000"));
            assertThat(r.getGarantia().cargoDanos()).isEqualByComparingTo("30000");
            assertThat(r.getGarantia().montoDevuelto()).isEqualByComparingTo("60000");
            assertThat(r.getGarantia().estado()).isEqualTo("DEVUELTO_PARCIAL");
            assertThatThrownBy(() -> r.liquidarReparacion(BigDecimal.TEN)).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Perdida: el depósito no se devuelve; si además hubo retraso, la multa queda como saldo")
        void perdida() {
            Reserva r = entregadaConDeposito(DEPOSITO);
            r.devolver("PERDIDO", null, r.getFechaFin().plusDays(1), TARIFA, RECARGO);
            Garantia g = r.getGarantia();
            assertThat(g.estado()).isEqualTo("NO_DEVUELTO");
            assertThat(g.montoDevuelto()).isEqualByComparingTo("0");
            assertThat(g.cargoDanos()).isEqualByComparingTo("90000");
            assertThat(g.saldoPendiente()).isEqualByComparingTo("67500");
            assertThat(g.depositoRetenido()).isEqualByComparingTo("90000");
        }

        @Test
        @DisplayName("Sin depósito: la multa por retraso queda completa como saldo pendiente")
        void sinDeposito() {
            Reserva r = entregadaConDeposito(BigDecimal.ZERO);
            assertThat(r.getGarantia().estado()).isEqualTo("SIN_DEPOSITO");
            r.devolver("BUENO", null, r.getFechaFin().plusDays(3), TARIFA, RECARGO);
            assertThat(r.getGarantia().estado()).isEqualTo("LIQUIDADO");
            assertThat(r.getGarantia().saldoPendiente()).isEqualByComparingTo("202500");
        }

        @Test
        @DisplayName("Reembolso antes de entregar: el depósito se devuelve con el pago")
        void reembolso() {
            Reserva r = reserva("ACEPTADA");
            r.confirmarPago(DEPOSITO);
            r.cancelarPorReembolso();
            assertThat(r.getGarantia().estado()).isEqualTo("REEMBOLSADO");
            assertThat(r.getGarantia().montoDevuelto()).isEqualByComparingTo("90000");
            assertThat(r.getGarantia().depositoRetenido()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Días de retraso: se cuentan desde la fecha fin; devolver antes no da días negativos")
        void diasRetraso() {
            Reserva r = reserva("ENTREGADA");
            assertThat(r.diasRetraso(r.getFechaFin().minusDays(2))).isZero();
            assertThat(r.diasRetraso(r.getFechaFin().plusDays(4))).isEqualTo(4);
            assertThat(Reserva.calcularMulta(0, TARIFA, RECARGO)).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("El depósito de garantía no puede ser negativo")
        void depositoNegativo() {
            assertThatThrownBy(() -> Garantia.retenida(new BigDecimal("-1"))).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Vencimiento por plazo (1 hora) o por fecha de inicio")
    class Vencimiento {

        private final LocalDateTime limite = AHORA.minusHours(1);

        @Test
        @DisplayName("PENDIENTE creada hace más de 1 hora vence")
        void pendienteSinRespuestaVence() {
            assertThat(reserva("PENDIENTE", HOY.plusDays(5), HOY.plusDays(6), AHORA.minusMinutes(61), null)
                    .debeVencer(HOY, limite)).isTrue();
        }

        @Test
        @DisplayName("PENDIENTE creada hace menos de 1 hora no vence")
        void pendienteRecienteNoVence() {
            assertThat(reserva("PENDIENTE", HOY.plusDays(5), HOY.plusDays(6), AHORA.minusMinutes(59), null)
                    .debeVencer(HOY, limite)).isFalse();
        }

        @Test
        @DisplayName("ACEPTADA cuenta el plazo desde la aceptación, no desde la creación")
        void aceptadaCuentaDesdeLaAceptacion() {
            Reserva aceptadaHacePoco = reserva("ACEPTADA", HOY.plusDays(5), HOY.plusDays(6),
                    AHORA.minusHours(5), AHORA.minusMinutes(30));
            Reserva aceptadaHaceMucho = reserva("ACEPTADA", HOY.plusDays(5), HOY.plusDays(6),
                    AHORA.minusHours(5), AHORA.minusMinutes(90));
            assertThat(aceptadaHacePoco.debeVencer(HOY, limite)).isFalse();
            assertThat(aceptadaHaceMucho.debeVencer(HOY, limite)).isTrue();
        }

        @Test
        @DisplayName("Vence si su fecha de inicio ya pasó, aunque esté dentro del plazo")
        void venceSiLaFechaDeInicioPaso() {
            assertThat(reserva("PENDIENTE", HOY.minusDays(1), HOY.plusDays(1), AHORA, null)
                    .debeVencer(HOY, limite)).isTrue();
        }

        @ParameterizedTest(name = "una reserva {0} nunca vence")
        @ValueSource(strings = {"CONFIRMADA", "ENTREGADA", "FINALIZADA", "RECHAZADA", "CANCELADA", "VENCIDA"})
        void soloVencenPendientesYAceptadas(String estado) {
            assertThat(reserva(estado, HOY.minusDays(3), HOY.minusDays(1), AHORA.minusDays(10), AHORA.minusDays(10))
                    .debeVencer(HOY, limite)).isFalse();
        }

        @Test
        void vencerCambiaElEstadoSoloEnEstadosPermitidos() {
            Reserva r = reserva("ACEPTADA");
            r.vencer();
            assertThat(r.getEstado()).isEqualTo("VENCIDA");
            assertThatThrownBy(() -> reserva("CONFIRMADA").vencer()).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Cancelación y modificación por el cliente")
    class CancelacionYModificacion {

        @ParameterizedTest(name = "se cancela una reserva {0}")
        @ValueSource(strings = {"PENDIENTE", "ACEPTADA"})
        void cancelaAntesDePagar(String estado) {
            Reserva r = reserva(estado);
            r.cancelar();
            assertThat(r.getEstado()).isEqualTo("CANCELADA");
        }

        @Test
        @DisplayName("Una reserva pagada solo se cancela con reembolso")
        void confirmadaPideReembolso() {
            assertThatThrownBy(() -> reserva("CONFIRMADA").cancelar())
                    .isInstanceOf(DomainException.class).hasMessageContaining("reembolsar");
            Reserva confirmada = reserva("CONFIRMADA");
            confirmada.cancelarPorReembolso();
            assertThat(confirmada.getEstado()).isEqualTo("CANCELADA");
        }

        @ParameterizedTest(name = "no se cancela una reserva {0}")
        @ValueSource(strings = {"ENTREGADA", "FINALIZADA", "RECHAZADA", "VENCIDA", "CANCELADA"})
        void noCancelaEstadosFinales(String estado) {
            assertThatThrownBy(() -> reserva(estado).cancelar()).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("El reembolso exige que la reserva siga CONFIRMADA")
        void reembolsoSoloDeConfirmadas() {
            assertThatThrownBy(() -> reserva("ENTREGADA").cancelarPorReembolso()).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Las fechas solo se cambian mientras está PENDIENTE")
        void modificaFechasSoloPendiente() {
            Reserva r = reserva("PENDIENTE");
            r.modificarFechas(HOY.plusDays(10), HOY.plusDays(12), HOY);
            assertThat(r.getFechaInicio()).isEqualTo(HOY.plusDays(10));
            assertThat(r.getFechaFin()).isEqualTo(HOY.plusDays(12));

            assertThatThrownBy(() -> reserva("ACEPTADA").modificarFechas(HOY.plusDays(10), HOY.plusDays(12), HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("pendientes");
        }
    }

    @Nested
    @DisplayName("Validación de fechas")
    class ValidacionFechas {

        @Test
        void aceptaHoyComoInicio() {
            Reserva.validarFechas(HOY, HOY, HOY);
        }

        @Test
        void rechazaFechasNulas() {
            assertThatThrownBy(() -> Reserva.validarFechas(null, HOY, HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("obligatorias");
        }

        @Test
        void rechazaInicioPosteriorAlFin() {
            assertThatThrownBy(() -> Reserva.validarFechas(HOY.plusDays(3), HOY.plusDays(1), HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("posterior");
        }

        @Test
        void rechazaInicioPasado() {
            assertThatThrownBy(() -> Reserva.validarFechas(HOY.minusDays(1), HOY, HOY))
                    .isInstanceOf(DomainException.class).hasMessageContaining("pasada");
        }
    }
}
