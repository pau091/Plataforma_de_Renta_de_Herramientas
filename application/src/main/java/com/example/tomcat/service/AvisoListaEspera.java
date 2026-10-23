package com.example.tomcat.service;

import com.example.tomcat.model.EntradaListaEspera;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Notificacion;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ListaEsperaRepositoryPort;
import com.example.tomcat.repository.NotificacionRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Lista de espera: cuando una reserva libera sus fechas (se cancela, se rechaza, vence o se reembolsa), avisa al primero
 * de la fila de esa herramienta cuyas fechas se cruzaban con las liberadas y ahora están completamente libres.
 * Debe llamarse después de guardar la reserva con su nuevo estado, dentro de la misma transacción.
 */
public class AvisoListaEspera {

    public static final String TIPO = "LISTA_ESPERA";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ListaEsperaRepositoryPort listaEsperaRepositoryPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final PoliticaVencimiento politica;

    public AvisoListaEspera(ListaEsperaRepositoryPort listaEsperaRepositoryPort,
                            NotificacionRepositoryPort notificacionRepositoryPort,
                            ReservaRepositoryPort reservaRepositoryPort,
                            HerramientaRepositoryPort herramientaRepositoryPort,
                            PoliticaVencimiento politica) {
        this.listaEsperaRepositoryPort = listaEsperaRepositoryPort;
        this.notificacionRepositoryPort = notificacionRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.politica = politica;
    }

    /** Devuelve la entrada notificada, si había alguien esperando esas fechas. */
    public Optional<EntradaListaEspera> fechasLiberadas(Reserva liberada) {
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(liberada.getHerramientaId()).orElse(null);
        if (herramienta == null || !herramienta.estaDisponible()) {
            return Optional.empty();
        }
        LocalDate hoy = politica.hoy();
        Optional<EntradaListaEspera> primera = listaEsperaRepositoryPort.listarActivasPorHerramienta(herramienta.getId()).stream()
                .filter(entrada -> "ESPERANDO".equals(entrada.getEstado()))
                .filter(entrada -> !entrada.getFechaInicio().isBefore(hoy))
                .filter(entrada -> entrada.seCruzaCon(liberada.getFechaInicio(), liberada.getFechaFin()))
                .filter(entrada -> !reservaRepositoryPort.existeSolape(herramienta.getId(), entrada.getFechaInicio(),
                        entrada.getFechaFin(), null))
                .findFirst();
        primera.ifPresent(entrada -> notificar(entrada, herramienta));
        return primera;
    }

    private void notificar(EntradaListaEspera entrada, Herramienta herramienta) {
        entrada.notificar(politica.ahoraUtc());
        listaEsperaRepositoryPort.guardar(entrada);
        notificacionRepositoryPort.guardar(Notificacion.nueva(
                entrada.getClienteId(),
                TIPO,
                "¡Se liberaron las fechas que esperabas!",
                "«" + herramienta.getNombre() + "» ya está libre del " + entrada.getFechaInicio().format(FECHA) + " al "
                        + entrada.getFechaFin().format(FECHA) + ". Resérvala pronto: otro cliente podría hacerlo antes.",
                herramienta.getId(),
                politica.ahoraUtc()
        ));
    }
}
