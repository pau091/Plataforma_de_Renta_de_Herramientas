package com.example.tomcat.service;

import com.example.tomcat.model.Factura;
import com.example.tomcat.model.Garantia;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static com.example.tomcat.service.Datos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Cerrar la reparación de una devolución con daño liquida el depósito con el costo real")
class MantenimientoDepositoTest {

    @Mock
    MantenimientoRepositoryPort mantenimientos;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    FacturaRepositoryPort facturas;

    GestionarMantenimientoService servicio;
    Reserva danada;

    @BeforeEach
    void preparar() {
        servicio = new GestionarMantenimientoService(mantenimientos, herramientas, reservas, new ControlAcceso(herramientas), facturas);
        when(herramientas.buscarPorId(HERRAMIENTA_ID)).thenReturn(Optional.of(herramienta("MANTENIMIENTO")));
        when(mantenimientos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        // Devuelta con daño a tiempo: el depósito de 90.000 espera el costo de la reparación.
        danada = new Reserva(1L, CLIENTE_ID, HERRAMIENTA_ID, HOY.minusDays(3), HOY.minusDays(1), "FINALIZADA", null, null,
                "DANADO", "Cable roto", AHORA_UTC.minusDays(4), null,
                Garantia.retenida(new BigDecimal("90000")).liquidarDevolucion("DANADO", 0, BigDecimal.ZERO));
        when(reservas.buscarPorId(1L)).thenReturn(Optional.of(danada));
    }

    @Test
    @DisplayName("Reparación de 30.000: se descuenta y se devuelven 60.000; la factura lo muestra")
    void descuentaElCostoReal() {
        when(mantenimientos.buscarPorId(5L)).thenReturn(Optional.of(
                new Mantenimiento(5L, HERRAMIENTA_ID, 1L, "Daño en devolución", null, null, null)));
        Factura factura = new Factura(9L, "FAC-000009", 9L, 1L, CLIENTE_ID, "Juan", "juan@correo.com", HERRAMIENTA_ID,
                "Taladro", HOY.minusDays(3), HOY.minusDays(1), 3, new BigDecimal("45000"), new BigDecimal("135000"),
                new BigDecimal("225000"), "TARJETA", null, null, danada.getGarantia());
        when(facturas.buscarPorReservaId(1L)).thenReturn(Optional.of(factura));

        servicio.cerrar(PROVEEDOR, 5L, new BigDecimal("30000"));

        assertThat(danada.getGarantia().estado()).isEqualTo("DEVUELTO_PARCIAL");
        assertThat(danada.getGarantia().cargoDanos()).isEqualByComparingTo("30000");
        assertThat(danada.getGarantia().montoDevuelto()).isEqualByComparingTo("60000");
        verify(reservas).guardar(danada);
        assertThat(factura.getGarantia().montoDevuelto()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("Un mantenimiento que no viene de una devolución no toca ningún depósito")
    void mantenimientoSinReserva() {
        when(mantenimientos.buscarPorId(6L)).thenReturn(Optional.of(
                new Mantenimiento(6L, HERRAMIENTA_ID, null, "Revisión preventiva", null, null, null)));
        servicio.cerrar(PROVEEDOR, 6L, new BigDecimal("10000"));
        verify(reservas, never()).guardar(any());
    }
}
