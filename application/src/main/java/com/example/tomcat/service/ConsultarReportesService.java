package com.example.tomcat.service;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.ConsultarReportesUseCase;
import com.example.tomcat.port.in.dto.ReporteClientesFrecuentes;
import com.example.tomcat.port.in.dto.ReporteDisponibilidad;
import com.example.tomcat.port.in.dto.ReporteHerramientasMasAlquiladas;
import com.example.tomcat.port.in.dto.ReporteIncidencias;
import com.example.tomcat.port.in.dto.ReporteIngresos;
import com.example.tomcat.port.in.dto.ReporteRentabilidad;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsultarReportesService implements ConsultarReportesUseCase {

    /** Estados que no representan un alquiler real: no cuentan en los rankings. */
    private static final Set<String> ESTADOS_SIN_ALQUILER = Set.of("RECHAZADA", "VENCIDA", "CANCELADA");

    private final PagoRepositoryPort pagoRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;

    public ConsultarReportesService(
            PagoRepositoryPort pagoRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            MantenimientoRepositoryPort mantenimientoRepositoryPort
    ) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
    }

    @Override
    public ReporteIngresos ingresos() {
        List<Pago> pagos = pagoRepositoryPort.listar().stream()
                .filter(pago -> "COMPLETADO".equals(pago.getEstadoPago()))
                .toList();
        BigDecimal total = pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReporteIngresos.IngresoPorMetodo> porMetodo = pagos.stream()
                .collect(Collectors.groupingBy(Pago::getMetodoPago))
                .entrySet().stream()
                .map(entry -> new ReporteIngresos.IngresoPorMetodo(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted(Comparator.comparing(ReporteIngresos.IngresoPorMetodo::total).reversed())
                .toList();
        return new ReporteIngresos(total, pagos.size(), porMetodo);
    }

    @Override
    public ReporteHerramientasMasAlquiladas herramientasMasAlquiladas() {
        List<Reserva> reservas = reservasActivas();
        Map<Long, Long> conteo = reservas.stream()
                .collect(Collectors.groupingBy(Reserva::getHerramientaId, Collectors.counting()));
        Map<Long, BigDecimal> ingresos = ingresosPorReserva();

        List<ReporteHerramientasMasAlquiladas.Item> items = herramientaRepositoryPort.listar().stream()
                .map(herramienta -> new ReporteHerramientasMasAlquiladas.Item(
                        herramienta.getId(),
                        herramienta.getNombre(),
                        herramienta.getEstado(),
                        conteo.getOrDefault(herramienta.getId(), 0L),
                        ingresosPorHerramienta(herramienta.getId(), reservas, ingresos)
                ))
                .sorted(Comparator.comparing(ReporteHerramientasMasAlquiladas.Item::alquileres).reversed())
                .toList();
        return new ReporteHerramientasMasAlquiladas(items);
    }

    @Override
    public ReporteClientesFrecuentes clientesFrecuentes() {
        List<Reserva> reservas = reservasActivas();
        Map<Long, Long> conteo = reservas.stream()
                .collect(Collectors.groupingBy(Reserva::getClienteId, Collectors.counting()));
        Map<Long, BigDecimal> ingresos = ingresosPorReserva();

        List<ReporteClientesFrecuentes.Item> items = usuarioRepositoryPort.listar().stream()
                .filter(usuario -> conteo.containsKey(usuario.getId()) || "CLIENTE".equals(usuario.getRol().name()))
                .map(usuario -> new ReporteClientesFrecuentes.Item(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        conteo.getOrDefault(usuario.getId(), 0L),
                        totalPagadoCliente(usuario, reservas, ingresos)
                ))
                .sorted(Comparator.comparing(ReporteClientesFrecuentes.Item::reservas).reversed())
                .toList();
        return new ReporteClientesFrecuentes(items);
    }

    @Override
    public ReporteDisponibilidad disponibilidad() {
        List<Herramienta> herramientas = herramientaRepositoryPort.listar();
        List<ReporteDisponibilidad.Item> items = herramientas.stream()
                .map(h -> new ReporteDisponibilidad.Item(
                        h.getId(),
                        h.getNombre(),
                        h.getEstado(),
                        h.getCategoriaId(),
                        h.getProveedorId(),
                        h.estaDisponible()
                ))
                .toList();
        long disponibles = herramientas.stream().filter(Herramienta::estaDisponible).count();
        long mantenimiento = herramientas.stream().filter(h -> "MANTENIMIENTO".equals(h.getEstado())).count();
        long fuera = herramientas.stream().filter(h -> "FUERA_DE_SERVICIO".equals(h.getEstado())).count();
        return new ReporteDisponibilidad(herramientas.size(), disponibles, mantenimiento, fuera, items);
    }

    @Override
    public ReporteRentabilidad rentabilidad() {
        Map<Long, BigDecimal> ingresos = ingresosPorReserva();
        Map<Long, List<Reserva>> reservasPagadas = reservaRepositoryPort.listar().stream()
                .filter(reserva -> ingresos.containsKey(reserva.getId()))
                .collect(Collectors.groupingBy(Reserva::getHerramientaId));
        Map<Long, BigDecimal> costos = mantenimientoRepositoryPort.listar().stream()
                .filter(m -> m.getCostoReparacion() != null)
                .collect(Collectors.groupingBy(
                        Mantenimiento::getHerramientaId,
                        Collectors.reducing(BigDecimal.ZERO, Mantenimiento::getCostoReparacion, BigDecimal::add)
                ));
        Map<Long, Usuario> usuarios = usuariosPorId();

        List<ReporteRentabilidad.Item> items = herramientaRepositoryPort.listar().stream()
                .map(herramienta -> {
                    List<Reserva> pagadas = reservasPagadas.getOrDefault(herramienta.getId(), List.of());
                    BigDecimal ingreso = pagadas.stream()
                            .map(reserva -> ingresos.get(reserva.getId()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal costo = costos.getOrDefault(herramienta.getId(), BigDecimal.ZERO);
                    BigDecimal utilidad = ingreso.subtract(costo);
                    Usuario proveedor = usuarios.get(herramienta.getProveedorId());
                    return new ReporteRentabilidad.Item(
                            herramienta.getId(),
                            herramienta.getNombre(),
                            herramienta.getProveedorId(),
                            proveedor == null ? null : proveedor.getNombre(),
                            pagadas.size(),
                            ingreso,
                            costo,
                            utilidad,
                            margen(utilidad, ingreso)
                    );
                })
                .sorted(Comparator.comparing(ReporteRentabilidad.Item::utilidad).reversed())
                .toList();

        BigDecimal ingresosTotales = items.stream().map(ReporteRentabilidad.Item::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costoTotal = items.stream().map(ReporteRentabilidad.Item::costoMantenimiento).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal utilidadTotal = ingresosTotales.subtract(costoTotal);
        return new ReporteRentabilidad(ingresosTotales, costoTotal, utilidadTotal, margen(utilidadTotal, ingresosTotales), items);
    }

    @Override
    public ReporteIncidencias incidencias() {
        List<Reserva> reservas = reservaRepositoryPort.listarPorEstadosDevolucion(List.of("DANADO", "PERDIDO"));
        Map<Long, Herramienta> herramientas = herramientaRepositoryPort.listar().stream()
                .collect(Collectors.toMap(Herramienta::getId, Function.identity()));
        Map<Long, Usuario> usuarios = usuariosPorId();

        List<ReporteIncidencias.Item> items = reservas.stream()
                .map(reserva -> {
                    Herramienta herramienta = herramientas.get(reserva.getHerramientaId());
                    Usuario cliente = usuarios.get(reserva.getClienteId());
                    // El mantenimiento que se abre al devolver con daño queda ligado a la reserva.
                    Mantenimiento mantenimiento = mantenimientoRepositoryPort.listarPorReserva(reserva.getId()).stream()
                            .max(Comparator.comparing(Mantenimiento::getFechaInicio))
                            .orElse(null);
                    return new ReporteIncidencias.Item(
                            reserva.getId(),
                            reserva.getHerramientaId(),
                            herramienta == null ? null : herramienta.getNombre(),
                            herramienta == null ? null : herramienta.getEstado(),
                            herramienta == null ? null : herramienta.getProveedorId(),
                            reserva.getClienteId(),
                            cliente == null ? null : cliente.getNombre(),
                            reserva.getEstadoDevolucion(),
                            reserva.getObservacionesDevolucion(),
                            reserva.getFechaDevolucion(),
                            mantenimiento == null ? null : mantenimiento.getId(),
                            mantenimiento == null ? null : mantenimiento.estaAbierto(),
                            mantenimiento == null ? null : mantenimiento.getCostoReparacion()
                    );
                })
                .sorted(Comparator.comparing(ReporteIncidencias.Item::fechaDevolucion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long danadas = reservas.stream().filter(Reserva::tieneDano).count();
        long perdidas = reservas.stream().filter(Reserva::estaPerdida).count();
        return new ReporteIncidencias(items.size(), danadas, perdidas, items);
    }

    private List<Reserva> reservasActivas() {
        return reservaRepositoryPort.listar().stream()
                .filter(reserva -> !ESTADOS_SIN_ALQUILER.contains(reserva.getEstado()))
                .toList();
    }

    private Map<Long, Usuario> usuariosPorId() {
        return usuarioRepositoryPort.listar().stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
    }

    /** Utilidad sobre ingresos en porcentaje, con 2 decimales; null si no hubo ingresos. */
    private BigDecimal margen(BigDecimal utilidad, BigDecimal ingresos) {
        if (ingresos.signum() == 0) {
            return null;
        }
        return utilidad.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP);
    }

    private Map<Long, BigDecimal> ingresosPorReserva() {
        return pagoRepositoryPort.listar().stream()
                .filter(pago -> "COMPLETADO".equals(pago.getEstadoPago()))
                .collect(Collectors.groupingBy(
                        Pago::getReservaId,
                        Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)
                ));
    }

    private BigDecimal ingresosPorHerramienta(Long herramientaId, List<Reserva> reservas, Map<Long, BigDecimal> ingresos) {
        return reservas.stream()
                .filter(reserva -> herramientaId.equals(reserva.getHerramientaId()))
                .map(reserva -> ingresos.getOrDefault(reserva.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalPagadoCliente(Usuario usuario, List<Reserva> reservas, Map<Long, BigDecimal> ingresos) {
        return reservas.stream()
                .filter(reserva -> usuario.getId().equals(reserva.getClienteId()))
                .map(reserva -> ingresos.getOrDefault(reserva.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
