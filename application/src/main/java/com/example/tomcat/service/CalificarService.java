package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Calificacion;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.CalificarUseCase;
import com.example.tomcat.port.in.dto.ResumenCalificaciones;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.CalificacionRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CalificarService implements CalificarUseCase {

    private final CalificacionRepositoryPort calificacionRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PoliticaVencimiento politica;

    public CalificarService(CalificacionRepositoryPort calificacionRepositoryPort, ReservaRepositoryPort reservaRepositoryPort,
                            HerramientaRepositoryPort herramientaRepositoryPort, UsuarioRepositoryPort usuarioRepositoryPort,
                            PoliticaVencimiento politica) {
        this.calificacionRepositoryPort = calificacionRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.politica = politica;
    }

    /** Solo quien alquiló (titular de la reserva), una vez y con el alquiler ya terminado. */
    @Override
    public Calificacion calificar(Solicitante solicitante, Long reservaId, Integer estrellasHerramienta,
                                  Integer estrellasProveedor, String comentario) {
        Reserva reserva = reservaRepositoryPort.buscarPorId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe."));
        if (!solicitante.esUsuario(reserva.getClienteId())) {
            throw new AccesoDenegadoException("Solo quien alquiló la herramienta puede calificarla.");
        }
        if (calificacionRepositoryPort.existePorReserva(reservaId)) {
            throw new ConflictoException("Este alquiler ya fue calificado.");
        }
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
        String nombre = usuarioRepositoryPort.buscarPorId(solicitante.id()).map(Usuario::getNombre).orElse(null);
        return calificacionRepositoryPort.guardar(Calificacion.de(reserva, herramienta, nombre, estrellasHerramienta,
                estrellasProveedor, comentario, politica.ahoraUtc()));
    }

    @Override
    public List<Calificacion> listarPorHerramienta(Long herramientaId) {
        herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
        return calificacionRepositoryPort.listarPorHerramienta(herramientaId);
    }

    @Override
    public List<Calificacion> mias(Solicitante solicitante) {
        return calificacionRepositoryPort.listarPorCliente(solicitante.id());
    }

    @Override
    public ResumenCalificaciones resumen() {
        List<Calificacion> todas = calificacionRepositoryPort.listar();
        return new ResumenCalificaciones(
                promedios(todas, Calificacion::getHerramientaId, Calificacion::getEstrellasHerramienta),
                promedios(todas, Calificacion::getProveedorId, Calificacion::getEstrellasProveedor));
    }

    /** Promedio por id, ignorando las reseñas que no calificaron ese aspecto (estrellas nulas). */
    private static List<ResumenCalificaciones.Promedio> promedios(List<Calificacion> calificaciones,
                                                                  Function<Calificacion, Long> id,
                                                                  Function<Calificacion, Integer> estrellas) {
        Map<Long, List<Integer>> porId = calificaciones.stream()
                .filter(c -> estrellas.apply(c) != null)
                .collect(Collectors.groupingBy(id, Collectors.mapping(estrellas, Collectors.toList())));
        return porId.entrySet().stream()
                .map(e -> new ResumenCalificaciones.Promedio(e.getKey(), promedio(e.getValue()), e.getValue().size()))
                .sorted(Comparator.comparing(ResumenCalificaciones.Promedio::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static BigDecimal promedio(List<Integer> valores) {
        int suma = valores.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(valores.size()), 1, RoundingMode.HALF_UP);
    }
}
