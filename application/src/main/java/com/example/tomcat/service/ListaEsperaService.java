package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.EntradaListaEspera;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.port.in.ListaEsperaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ListaEsperaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.time.LocalDate;
import java.util.List;

public class ListaEsperaService implements ListaEsperaUseCase {

    private final ListaEsperaRepositoryPort listaEsperaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final PoliticaVencimiento politica;

    public ListaEsperaService(ListaEsperaRepositoryPort listaEsperaRepositoryPort, HerramientaRepositoryPort herramientaRepositoryPort,
                              ReservaRepositoryPort reservaRepositoryPort, PoliticaVencimiento politica) {
        this.listaEsperaRepositoryPort = listaEsperaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.politica = politica;
    }

    /** Solo tiene sentido si esas fechas están ocupadas; si están libres, se reserva directamente. */
    @Override
    public EntradaListaEspera anotarse(Solicitante solicitante, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        if (!solicitante.puedeAlquilar()) {
            throw new AccesoDenegadoException("Solo clientes o proveedores (alquilando equipos de otros) pueden anotarse.");
        }
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
        if (solicitante.esUsuario(herramienta.getProveedorId())) {
            throw new DomainException("No puede anotarse para sus propias herramientas.");
        }
        EntradaListaEspera entrada = EntradaListaEspera.nueva(herramientaId, solicitante.id(), fechaInicio, fechaFin,
                politica.hoy(), politica.ahoraUtc());
        if (herramienta.estaDisponible() && !reservaRepositoryPort.existeSolape(herramientaId, fechaInicio, fechaFin, null)) {
            throw new DomainException("La herramienta está libre en esas fechas: puede reservarla directamente.");
        }
        boolean yaAnotado = listaEsperaRepositoryPort.listarPorCliente(solicitante.id()).stream()
                .anyMatch(e -> e.activa() && herramientaId.equals(e.getHerramientaId()) && e.seCruzaCon(fechaInicio, fechaFin));
        if (yaAnotado) {
            throw new ConflictoException("Ya está en la lista de espera de esta herramienta para esas fechas.");
        }
        return listaEsperaRepositoryPort.guardar(entrada);
    }

    @Override
    public List<EntradaListaEspera> mias(Solicitante solicitante) {
        return listaEsperaRepositoryPort.listarPorCliente(solicitante.id());
    }

    @Override
    public EntradaListaEspera cancelar(Solicitante solicitante, Long id) {
        EntradaListaEspera entrada = listaEsperaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La entrada de la lista de espera no existe."));
        if (!solicitante.esUsuario(entrada.getClienteId()) && !solicitante.esAdmin()) {
            throw new AccesoDenegadoException("Solo quien se anotó puede salir de la lista de espera.");
        }
        entrada.cancelar();
        return listaEsperaRepositoryPort.guardar(entrada);
    }
}
