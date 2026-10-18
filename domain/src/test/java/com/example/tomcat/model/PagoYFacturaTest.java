package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pago y Factura: método, reembolso y anulación")
class PagoYFacturaTest {

    private static Pago pago(String estado) {
        return new Pago(1L, 7L, new BigDecimal("135000"), "TARJETA", estado, null);
    }

    @ParameterizedTest(name = "\"{0}\" se normaliza a {1}")
    @CsvSource({"tarjeta,TARJETA", " Transferencia ,TRANSFERENCIA", "EFECTIVO,EFECTIVO"})
    void normalizaMetodosValidos(String recibido, String esperado) {
        assertThat(Pago.normalizarMetodo(recibido)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "método inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"BITCOIN", "cheque"})
    void rechazaMetodosInvalidos(String metodo) {
        assertThatThrownBy(() -> Pago.normalizarMetodo(metodo))
                .isInstanceOf(DomainException.class).hasMessageContaining("TARJETA, TRANSFERENCIA o EFECTIVO");
    }

    @Test
    @DisplayName("Sin estado nace PENDIENTE y con fecha de pago")
    void valoresPorDefecto() {
        Pago p = new Pago(null, 1L, BigDecimal.TEN, "EFECTIVO", null, null);
        assertThat(p.getEstadoPago()).isEqualTo("PENDIENTE");
        assertThat(p.getFechaPago()).isNotNull();
        assertThat(p.estaCompletado()).isFalse();
    }

    @Test
    @DisplayName("Corregir el método no cambia el monto")
    void corrigeElMetodo() {
        Pago p = pago("COMPLETADO");
        p.cambiarMetodo("efectivo");
        assertThat(p.getMetodoPago()).isEqualTo("EFECTIVO");
        assertThat(p.getMonto()).isEqualByComparingTo("135000");
    }

    @Test
    @DisplayName("Solo se reembolsan pagos completados, una sola vez")
    void reembolsaUnaVez() {
        Pago p = pago("COMPLETADO");
        p.reembolsar();
        assertThat(p.getEstadoPago()).isEqualTo("REEMBOLSADO");
        assertThat(p.estaCompletado()).isFalse();
        assertThatThrownBy(p::reembolsar).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> p.cambiarMetodo("TARJETA")).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("La factura nace VIGENTE y se puede anular y corregir")
    void facturaVigenteYAnulada() {
        Factura f = new Factura(1L, "FAC-000001", 1L, 7L, 12L, "Juan Cliente", "juan@correo.com", 3L, "Taladro",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 3, new BigDecimal("45000"),
                new BigDecimal("135000"), new BigDecimal("135000"), "TARJETA", null, null);
        assertThat(f.getEstado()).isEqualTo("VIGENTE");
        assertThat(f.getFechaEmision()).isNotNull();

        f.cambiarMetodoPago("EFECTIVO");
        f.anular();
        assertThat(f.getMetodoPago()).isEqualTo("EFECTIVO");
        assertThat(f.estaAnulada()).isTrue();
    }
}
