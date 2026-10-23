package com.example.tomcat.service;

import com.example.tomcat.model.Garantia;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.port.in.dto.ReporteClientesFrecuentes;
import com.example.tomcat.port.in.dto.ReporteIncidencias;
import com.example.tomcat.port.in.dto.ReporteIngresos;
import com.example.tomcat.port.in.dto.ReporteMultas;
import com.example.tomcat.port.in.dto.ReporteRentabilidad;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.tomcat.service.Datos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Reportes: ingresos, rentabilidad, clientes frecuentes e incidencias")
class ConsultarReportesServiceTest {

    @Mock
    PagoRepositoryPort pagos;
    @Mock
    ReservaRepositoryPort reservas;
    @Mock
    HerramientaRepositoryPort herramientas;
    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    MantenimientoRepositoryPort mantenimientos;

    ConsultarReportesService reportes;
    Herramienta taladro;
    Herramienta andamio;

    private static Reserva reservaDe(Long id, Long clienteId, Long herramientaId, String estado) {
        return new Reserva(id, clienteId, herramientaId, HOY.minusDays(10), HOY.minusDays(8), estado,
                null, null, null, null, AHORA_UTC.minusDays(12), null);
    }

    @BeforeEach
    void preparar() {
        reportes = new ConsultarReportesService(pagos, reservas, herramientas, usuarios, mantenimientos);
        taladro = herramienta("DISPONIBLE"); // id 1, proveedor 2
        andamio = new Herramienta(2L, PROVEEDOR_ID, 3L, "Andamio", "2x2", new BigDecimal("25000"), "MANTENIMIENTO", null);
        when(herramientas.listar()).thenReturn(List.of(taladro, andamio));
        when(usuarios.listar()).thenReturn(List.of(
                usuario(PROVEEDOR_ID, "Carlos Proveedor", Rol.PROVEEDOR, true),
                usuario(CLIENTE_ID, "Juan Cliente", Rol.CLIENTE, true),
                usuario(OTRO_CLIENTE_ID, "Ana Cliente", Rol.CLIENTE, true)));
        when(reservas.listar()).thenReturn(List.of(
                reservaDe(1L, CLIENTE_ID, 1L, "FINALIZADA"),
                reservaDe(2L, CLIENTE_ID, 1L, "FINALIZADA"),
                reservaDe(3L, OTRO_CLIENTE_ID, 2L, "FINALIZADA"),
                reservaDe(4L, OTRO_CLIENTE_ID, 1L, "CANCELADA"),   // pago reembolsado
                reservaDe(5L, OTRO_CLIENTE_ID, 2L, "RECHAZADA")));
        when(pagos.listar()).thenReturn(List.of(
                pago(1L, 1L, "135000", "COMPLETADO"),
                pago(2L, 2L, "90000", "COMPLETADO"),
                pago(3L, 3L, "50000", "COMPLETADO"),
                pago(4L, 4L, "45000", "REEMBOLSADO")));
        when(mantenimientos.listar()).thenReturn(List.of(
                new Mantenimiento(1L, 1L, null, "Revisión", new BigDecimal("25000"), null, LocalDateTime.now()),
                new Mantenimiento(2L, 2L, 3L, "Daño", new BigDecimal("80000"), null, LocalDateTime.now()),
                new Mantenimiento(3L, 2L, null, "Abierto, sin costo aún", null, null, null)));
    }

    @Test
    @DisplayName("Los ingresos solo cuentan pagos COMPLETADO (no los reembolsados)")
    void ingresosSinReembolsos() {
        ReporteIngresos r = reportes.ingresos();
        assertThat(r.totalIngresos()).isEqualByComparingTo("275000");
        assertThat(r.pagosCompletados()).isEqualTo(3);
    }

    @Test
    @DisplayName("El depósito en custodia no es ingreso; la multa sí, y lo no cubierto queda como saldo por cobrar")
    void ingresosConDepositoYMulta() {
        Garantia liquidada = Garantia.retenida(new BigDecimal("90000"))
                .liquidarDevolucion("BUENO", 1, new BigDecimal("67500"));            // devuelve 22.500
        Garantia enCustodia = Garantia.retenida(new BigDecimal("50000"));
        when(reservas.listar()).thenReturn(List.of(
                new Reserva(6L, CLIENTE_ID, 1L, HOY.minusDays(4), HOY.minusDays(3), "FINALIZADA", null, null, "BUENO", null,
                        AHORA_UTC.minusDays(5), null, liquidada),
                new Reserva(7L, CLIENTE_ID, 2L, HOY, HOY.plusDays(3), "ENTREGADA", null, null, null, null,
                        AHORA_UTC.minusDays(1), null, enCustodia)));
        when(pagos.listar()).thenReturn(List.of(
                pago(6L, 6L, "180000", "COMPLETADO"),    // 90.000 de alquiler + 90.000 de depósito
                pago(7L, 7L, "150000", "COMPLETADO")));  // 100.000 de alquiler + 50.000 de depósito

        ReporteIngresos r = reportes.ingresos();

        assertThat(r.ingresosAlquiler()).isEqualByComparingTo("190000");
        assertThat(r.ingresosMultas()).isEqualByComparingTo("67500");
        assertThat(r.totalIngresos()).isEqualByComparingTo("257500");
        assertThat(r.depositosEnCustodia()).isEqualByComparingTo("50000");
        assertThat(r.depositosDevueltos()).isEqualByComparingTo("22500");

        ReporteMultas multas = reportes.multas();
        assertThat(multas.devolucionesTardias()).isEqualTo(1);
        assertThat(multas.totalMultas()).isEqualByComparingTo("67500");
        assertThat(multas.items().get(0).herramientaNombre()).isEqualTo("Taladro Percutor");
    }

    @Test
    @DisplayName("Rentabilidad = ingresos − costo de mantenimiento, con margen; ordenada por utilidad")
    void rentabilidad() {
        ReporteRentabilidad r = reportes.rentabilidad();

        assertThat(r.ingresosTotales()).isEqualByComparingTo("275000");
        assertThat(r.costoMantenimientoTotal()).isEqualByComparingTo("105000");
        assertThat(r.utilidadTotal()).isEqualByComparingTo("170000");
        assertThat(r.margenPorcentaje()).isEqualByComparingTo("61.82");

        ReporteRentabilidad.HerramientaRentabilidad primero = r.items().get(0);
        assertThat(primero.nombre()).isEqualTo("Taladro Percutor");
        assertThat(primero.alquileresPagados()).isEqualTo(2);
        assertThat(primero.ingresos()).isEqualByComparingTo("225000");
        assertThat(primero.utilidad()).isEqualByComparingTo("200000");
        assertThat(primero.proveedorNombre()).isEqualTo("Carlos Proveedor");

        ReporteRentabilidad.HerramientaRentabilidad andamioItem = r.items().get(1);
        assertThat(andamioItem.utilidad()).isEqualByComparingTo("-30000");
        assertThat(andamioItem.margenPorcentaje()).isEqualByComparingTo("-60.00");
    }

    @Test
    @DisplayName("Sin ingresos el margen es nulo (no se divide entre cero)")
    void margenSinIngresos() {
        when(pagos.listar()).thenReturn(List.of());
        ReporteRentabilidad r = reportes.rentabilidad();
        assertThat(r.margenPorcentaje()).isNull();
        assertThat(r.utilidadTotal()).isEqualByComparingTo("-105000");
    }

    @Test
    @DisplayName("Clientes frecuentes no cuenta reservas canceladas ni rechazadas")
    void clientesFrecuentes() {
        ReporteClientesFrecuentes r = reportes.clientesFrecuentes();
        assertThat(r.items()).extracting(ReporteClientesFrecuentes.ClienteFrecuente::nombre)
                .startsWith("Juan Cliente");
        assertThat(r.items().get(0).reservas()).isEqualTo(2);
        assertThat(r.items().get(1).reservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Incidencias: devoluciones dañadas o perdidas con su mantenimiento")
    void incidencias() {
        Reserva danada = new Reserva(3L, OTRO_CLIENTE_ID, 2L, HOY.minusDays(10), HOY.minusDays(8), "FINALIZADA",
                AHORA_UTC.minusDays(10), AHORA_UTC.minusDays(8), "DANADO", "Tubo doblado", AHORA_UTC.minusDays(12), null);
        Reserva perdida = new Reserva(6L, CLIENTE_ID, 1L, HOY.minusDays(6), HOY.minusDays(5), "FINALIZADA",
                AHORA_UTC.minusDays(6), AHORA_UTC.minusDays(5), "PERDIDO", null, AHORA_UTC.minusDays(7), null);
        when(reservas.listarPorEstadosDevolucion(List.of("DANADO", "PERDIDO"))).thenReturn(List.of(danada, perdida));
        when(mantenimientos.listarPorReserva(anyLong())).thenReturn(List.of());
        when(mantenimientos.listarPorReserva(3L)).thenReturn(List.of(
                new Mantenimiento(2L, 2L, 3L, "Daño", new BigDecimal("80000"), null, LocalDateTime.now())));

        ReporteIncidencias r = reportes.incidencias();

        assertThat(r.total()).isEqualTo(2);
        assertThat(r.danadas()).isEqualTo(1);
        assertThat(r.perdidas()).isEqualTo(1);
        // La más reciente primero.
        assertThat(r.items()).extracting(ReporteIncidencias.Incidencia::reservaId).containsExactly(6L, 3L);
        ReporteIncidencias.Incidencia conMantenimiento = r.items().get(1);
        assertThat(conMantenimiento.mantenimientoId()).isEqualTo(2L);
        assertThat(conMantenimiento.mantenimientoAbierto()).isFalse();
        assertThat(conMantenimiento.costoReparacion()).isEqualByComparingTo("80000");
        assertThat(conMantenimiento.clienteNombre()).isEqualTo("Ana Cliente");
        assertThat(r.items().get(0).mantenimientoId()).isNull();
    }
}
