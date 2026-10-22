package com.example.tomcat;

import com.example.tomcat.adapter.out.persistence.entity.ReservaEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataReservaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integración: cruce de fechas y control de concurrencia")
class ConcurrenciaIT extends IntegracionBase {

    private static final int PETICIONES_SIMULTANEAS = 8;

    @Autowired
    private SpringDataReservaRepository reservaRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Con una reserva del día 10 al 15, se rechaza todo rango que la cruce y se acepta el siguiente libre")
    void solapamientoDeFechas() throws Exception {
        long herramienta = nuevaHerramienta("Taladro solape", 20000);
        reservar(CLIENTE, herramienta, dia(10), dia(15));

        for (String[] rango : new String[][]{
                {dia(12), dia(18)},   // empieza dentro
                {dia(5), dia(10)},    // termina el primer día
                {dia(15), dia(20)},   // empieza el último día
                {dia(11), dia(13)},   // queda adentro
                {dia(8), dia(20)}}) { // la envuelve
            var r = esperar(POST("/api/reservas", OTRO_CLIENTE, Map.of("herramientaId", herramienta,
                    "fechaInicio", rango[0], "fechaFin", rango[1])), 409);
            assertThat(r.get("message").asText()).contains("ya tiene una reserva en esas fechas");
        }

        reservar(OTRO_CLIENTE, herramienta, dia(16), dia(20));
        reservar(OTRO_CLIENTE, herramienta, dia(3), dia(9));
    }

    @Test
    @DisplayName("Bloqueo pesimista: varios clientes reservan las mismas fechas a la vez y solo uno lo logra")
    void reservasSimultaneas() throws Exception {
        long herramienta = nuevaHerramienta("Mezcladora concurrencia", 50000);
        token(CLIENTE);
        token(OTRO_CLIENTE);

        List<Integer> estados = enParalelo(i -> POST("/api/reservas", i % 2 == 0 ? CLIENTE : OTRO_CLIENTE,
                Map.of("herramientaId", herramienta, "fechaInicio", dia(30), "fechaFin", dia(32))));

        assertThat(estados).containsOnly(201, 409);
        assertThat(estados).filteredOn(s -> s == 201).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservas WHERE herramienta_id = ? AND estado = 'PENDIENTE'",
                Integer.class, herramienta)).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrencia en el pago: dos pagos simultáneos de la misma reserva generan un solo pago y una factura")
    void pagosSimultaneos() throws Exception {
        long herramienta = nuevaHerramienta("Compresor concurrencia", 30000);
        long reserva = reservar(CLIENTE, herramienta, dia(40), dia(41));
        esperar(POST("/api/reservas/" + reserva + "/aceptar", PROVEEDOR, null), 200);
        token(CLIENTE);

        List<Integer> estados = enParalelo(i -> POST("/api/pagos", CLIENTE,
                Map.of("reservaId", reserva, "monto", 60000, "metodoPago", "TARJETA")));

        assertThat(estados).containsOnly(201, 409);
        assertThat(estados).filteredOn(s -> s == 201).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE reserva_id = ?", Integer.class, reserva)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM facturas f JOIN pagos p ON p.id = f.pago_id WHERE p.reserva_id = ?",
                Integer.class, reserva)).isEqualTo(1);
    }

    @Test
    @DisplayName("Bloqueo optimista (@Version): guardar una copia desactualizada de una reserva falla en vez de pisar el cambio")
    void versionDeLaReserva() throws Exception {
        long id = reservar(CLIENTE, nuevaHerramienta("Andamio version", 15000), dia(50), dia(51));
        ReservaEntity copiaA = reservaRepository.findById(id).orElseThrow();
        ReservaEntity copiaB = reservaRepository.findById(id).orElseThrow();
        long versionInicial = copiaA.getVersion();

        copiaA.setEstado("RECHAZADA");
        assertThat(reservaRepository.save(copiaA).getVersion()).isEqualTo(versionInicial + 1);

        copiaB.setEstado("CANCELADA");
        assertThatThrownBy(() -> reservaRepository.save(copiaB)).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(reservaRepository.findById(id).orElseThrow().getEstado()).isEqualTo("RECHAZADA");
    }

    /** Lanza todas las peticiones al mismo tiempo (una barrera las libera juntas) y devuelve sus códigos HTTP. */
    private List<Integer> enParalelo(Peticion peticion) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(PETICIONES_SIMULTANEAS);
        try {
            CountDownLatch salida = new CountDownLatch(1);
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < PETICIONES_SIMULTANEAS; i++) {
                MockHttpServletRequestBuilder builder = peticion.crear(i);
                Callable<Integer> tarea = () -> {
                    salida.await();
                    return mvc.perform(builder).andReturn().getResponse().getStatus();
                };
                resultados.add(hilos.submit(tarea));
            }
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                estados.add(resultado.get());
            }
            return estados;
        } finally {
            hilos.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface Peticion {
        MockHttpServletRequestBuilder crear(int indice) throws Exception;
    }
}
