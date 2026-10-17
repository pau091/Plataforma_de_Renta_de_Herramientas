package com.example.tomcat.service;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.ConsultarReportesUseCase;
import com.example.tomcat.port.in.dto.ReporteClientesFrecuentes;
import com.example.tomcat.port.in.dto.ReporteDisponibilidad;
import com.example.tomcat.port.in.dto.ReporteHerramientasMasAlquiladas;
import com.example.tomcat.port.in.dto.ReporteIngresos;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsultarReportesService implements ConsultarReportesUseCase {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public ConsultarReportesService(
            PagoRepositoryPort pagoRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort
    ) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
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

    private List<Reserva> reservasActivas() {
        return reservaRepositoryPort.listar().stream()
                .filter(reserva -> !"RECHAZADA".equals(reserva.getEstado()))
                .toList();
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
