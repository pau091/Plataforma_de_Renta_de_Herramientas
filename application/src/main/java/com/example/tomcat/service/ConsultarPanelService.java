package com.example.tomcat.service;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.port.in.ConsultarPanelUseCase;
import com.example.tomcat.port.in.dto.Panel;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsultarPanelService implements ConsultarPanelUseCase {

    private static final Set<String> ESTADOS_EN_CURSO = Set.of("PENDIENTE", "ACEPTADA", "ENTREGADA");

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final PagoRepositoryPort pagoRepositoryPort;
    private final FacturaRepositoryPort facturaRepositoryPort;
    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;

    public ConsultarPanelService(
            UsuarioRepositoryPort usuarioRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            PagoRepositoryPort pagoRepositoryPort,
            FacturaRepositoryPort facturaRepositoryPort,
            MantenimientoRepositoryPort mantenimientoRepositoryPort
    ) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.facturaRepositoryPort = facturaRepositoryPort;
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
    }

    @Override
    public Panel obtener(Solicitante solicitante) {
        if (solicitante.esCliente()) {
            return panelCliente(solicitante.id());
        }
        if (solicitante.esProveedor()) {
            return panelProveedor(solicitante.id());
        }
        return panelAdmin();
    }

    private Panel.Cliente panelCliente(Long clienteId) {
        List<Reserva> reservas = reservaRepositoryPort.listarPorCliente(clienteId);
        List<Reserva> proximas = reservas.stream()
                .filter(r -> ESTADOS_EN_CURSO.contains(r.getEstado()))
                .sorted(Comparator.comparing(Reserva::getFechaInicio))
                .toList();
        return new Panel.Cliente(
                Rol.CLIENTE.name(),
                contar(reservas, Reserva::getEstado),
                proximas,
                totalCompletado(pagoRepositoryPort.listarPorReservas(ids(reservas))),
                facturaRepositoryPort.listarPorCliente(clienteId).size()
        );
    }

    private Panel.Proveedor panelProveedor(Long proveedorId) {
        List<Herramienta> herramientas = herramientaRepositoryPort.listarPorProveedor(proveedorId);
        List<Reserva> reservas = reservaRepositoryPort.listarPorProveedor(proveedorId);
        List<Reserva> pendientes = reservas.stream()
                .filter(r -> "PENDIENTE".equals(r.getEstado()))
                .sorted(Comparator.comparing(Reserva::getFechaInicio))
                .toList();
        long mantenimientosAbiertos = mantenimientoRepositoryPort
                .listarPorHerramientas(herramientas.stream().map(Herramienta::getId).toList()).stream()
                .filter(Mantenimiento::estaAbierto)
                .count();
        return new Panel.Proveedor(
                Rol.PROVEEDOR.name(),
                herramientas.size(),
                contar(herramientas, Herramienta::getEstado),
                contar(reservas, Reserva::getEstado),
                pendientes,
                totalCompletado(pagoRepositoryPort.listarPorReservas(ids(reservas))),
                mantenimientosAbiertos
        );
    }

    private Panel.Admin panelAdmin() {
        List<Pago> completados = pagoRepositoryPort.listar().stream()
                .filter(p -> "COMPLETADO".equals(p.getEstadoPago()))
                .toList();
        return new Panel.Admin(
                Rol.ADMIN.name(),
                usuarioRepositoryPort.listarPorRol(Rol.CLIENTE).size(),
                usuarioRepositoryPort.listarPorRol(Rol.PROVEEDOR).size(),
                contar(herramientaRepositoryPort.listar(), Herramienta::getEstado),
                contar(reservaRepositoryPort.listar(), Reserva::getEstado),
                totalCompletado(completados),
                completados.size(),
                mantenimientoRepositoryPort.listar().stream().filter(Mantenimiento::estaAbierto).count()
        );
    }

    private static <T> Map<String, Long> contar(Collection<T> elementos, Function<T, String> clave) {
        return elementos.stream().collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()));
    }

    private static List<Long> ids(List<Reserva> reservas) {
        return reservas.stream().map(Reserva::getId).toList();
    }

    private static BigDecimal totalCompletado(List<Pago> pagos) {
        return pagos.stream()
                .filter(p -> "COMPLETADO".equals(p.getEstadoPago()))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
