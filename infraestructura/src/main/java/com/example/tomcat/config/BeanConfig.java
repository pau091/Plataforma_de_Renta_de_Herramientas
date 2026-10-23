package com.example.tomcat.config;

import com.example.tomcat.port.in.AutenticarUsuarioUseCase;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.port.in.ConsultarReportesUseCase;
import com.example.tomcat.port.in.ConsultarPanelUseCase;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.GestionarCategoriaUseCase;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import com.example.tomcat.port.in.GestionarPagoUseCase;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.port.in.CalificarUseCase;
import com.example.tomcat.port.in.ListaEsperaUseCase;
import com.example.tomcat.port.in.NotificacionesUseCase;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.port.in.SancionarUsuarioUseCase;
import com.example.tomcat.port.in.VencerReservasUseCase;
import com.example.tomcat.port.out.AlmacenamientoImagenPort;
import com.example.tomcat.port.out.ListaNegraTokensPort;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.CategoriaRepositoryPort;
import com.example.tomcat.repository.CalificacionRepositoryPort;
import com.example.tomcat.repository.ListaEsperaRepositoryPort;
import com.example.tomcat.repository.NotificacionRepositoryPort;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.service.AutenticarUsuarioService;
import com.example.tomcat.service.ConsultarDisponibilidadService;
import com.example.tomcat.service.ConsultarPanelService;
import com.example.tomcat.service.ConsultarReportesService;
import com.example.tomcat.service.ControlAcceso;
import com.example.tomcat.service.CrearReservaService;
import com.example.tomcat.service.GestionarCategoriaService;
import com.example.tomcat.service.GestionarFacturaService;
import com.example.tomcat.service.GestionarHerramientaService;
import com.example.tomcat.service.GestionarMantenimientoService;
import com.example.tomcat.service.GestionarPagoService;
import com.example.tomcat.service.GestionarReservaService;
import com.example.tomcat.service.AvisoListaEspera;
import com.example.tomcat.service.CalificarService;
import com.example.tomcat.service.ListaEsperaService;
import com.example.tomcat.service.NotificacionesService;
import com.example.tomcat.service.GestionarUsuarioService;
import com.example.tomcat.service.ListarPagosService;
import com.example.tomcat.service.PoliticaVencimiento;
import com.example.tomcat.service.ProcesarPagoService;
import com.example.tomcat.service.RegistrarUsuarioService;
import com.example.tomcat.service.SancionarUsuarioService;
import com.example.tomcat.service.VencerReservasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Crea los casos de uso de la capa application (que no conoce Spring) y los envuelve en
 * {@link TransaccionalProxy}: cada llamada a un caso de uso es una transacción completa.
 */
@Configuration
public class BeanConfig {

    private static final boolean ESCRITURA = false;
    private static final boolean LECTURA = true;

    private final PlatformTransactionManager tx;

    public BeanConfig(PlatformTransactionManager transactionManager) {
        this.tx = transactionManager;
    }

    /** Reloj con la zona horaria del negocio: define qué día es "hoy" para reservas y vencimientos. */
    @Bean
    public Clock reloj(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }

    /** Plazo máximo para que una reserva PENDIENTE se acepte o una ACEPTADA se pague antes de vencer. */
    @Bean
    public PoliticaVencimiento politicaVencimiento(Clock reloj, @Value("${app.reservas.plazo-maximo}") Duration plazoMaximo) {
        return new PoliticaVencimiento(reloj, plazoMaximo);
    }

    @Bean
    public ControlAcceso controlAcceso(HerramientaRepositoryPort herramientaRepository) {
        return new ControlAcceso(herramientaRepository);
    }

    /** Avisa al primero de la lista de espera cuando una reserva libera sus fechas (lo usan varios casos de uso). */
    @Bean
    public AvisoListaEspera avisoListaEspera(ListaEsperaRepositoryPort listaEsperaRepository,
                                             NotificacionRepositoryPort notificacionRepository,
                                             ReservaRepositoryPort reservaRepository,
                                             HerramientaRepositoryPort herramientaRepository,
                                             PoliticaVencimiento politicaVencimiento) {
        return new AvisoListaEspera(listaEsperaRepository, notificacionRepository, reservaRepository, herramientaRepository,
                politicaVencimiento);
    }

    @Bean
    public ListaEsperaUseCase listaEsperaUseCase(ListaEsperaRepositoryPort listaEsperaRepository,
                                                 HerramientaRepositoryPort herramientaRepository,
                                                 ReservaRepositoryPort reservaRepository,
                                                 PoliticaVencimiento politicaVencimiento) {
        return TransaccionalProxy.envolver(ListaEsperaUseCase.class,
                new ListaEsperaService(listaEsperaRepository, herramientaRepository, reservaRepository, politicaVencimiento),
                tx, ESCRITURA);
    }

    @Bean
    public NotificacionesUseCase notificacionesUseCase(NotificacionRepositoryPort notificacionRepository) {
        return TransaccionalProxy.envolver(NotificacionesUseCase.class, new NotificacionesService(notificacionRepository),
                tx, ESCRITURA);
    }

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            PasswordHasherPort passwordHasherPort
    ) {
        return TransaccionalProxy.envolver(RegistrarUsuarioUseCase.class,
                new RegistrarUsuarioService(usuarioRepository, passwordHasherPort), tx, ESCRITURA);
    }

    @Bean
    public AutenticarUsuarioUseCase autenticarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            PasswordHasherPort passwordHasherPort,
            Clock reloj
    ) {
        return TransaccionalProxy.envolver(AutenticarUsuarioUseCase.class,
                new AutenticarUsuarioService(usuarioRepository, passwordHasherPort, reloj), tx, LECTURA);
    }

    @Bean
    public GestionarUsuarioUseCase gestionarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository,
            PasswordHasherPort passwordHasherPort,
            ListaNegraTokensPort listaNegraTokensPort,
            @Value("${app.admin-principal}") String emailAdminPrincipal
    ) {
        return TransaccionalProxy.envolver(GestionarUsuarioUseCase.class,
                new GestionarUsuarioService(usuarioRepository, herramientaRepository, reservaRepository, passwordHasherPort,
                        listaNegraTokensPort, emailAdminPrincipal),
                tx, ESCRITURA);
    }

    @Bean
    public CalificarUseCase calificarUseCase(
            CalificacionRepositoryPort calificacionRepository,
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            PoliticaVencimiento politicaVencimiento
    ) {
        return TransaccionalProxy.envolver(CalificarUseCase.class,
                new CalificarService(calificacionRepository, reservaRepository, herramientaRepository, usuarioRepository,
                        politicaVencimiento),
                tx, ESCRITURA);
    }

    @Bean
    public SancionarUsuarioUseCase sancionarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            ListaNegraTokensPort listaNegraTokensPort,
            Clock reloj
    ) {
        return TransaccionalProxy.envolver(SancionarUsuarioUseCase.class,
                new SancionarUsuarioService(usuarioRepository, listaNegraTokensPort, reloj), tx, ESCRITURA);
    }

    @Bean
    public GestionarHerramientaUseCase gestionarHerramientaUseCase(
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            CategoriaRepositoryPort categoriaRepository,
            ReservaRepositoryPort reservaRepository,
            MantenimientoRepositoryPort mantenimientoRepository,
            AlmacenamientoImagenPort almacenamientoImagenPort,
            ControlAcceso controlAcceso
    ) {
        return TransaccionalProxy.envolver(GestionarHerramientaUseCase.class,
                new GestionarHerramientaService(herramientaRepository, usuarioRepository, categoriaRepository,
                        reservaRepository, mantenimientoRepository, almacenamientoImagenPort, controlAcceso),
                tx, ESCRITURA);
    }

    @Bean
    public CrearReservaUseCase crearReservaUseCase(
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            ReservaRepositoryPort reservaRepository,
            PoliticaVencimiento politicaVencimiento,
            ListaEsperaRepositoryPort listaEsperaRepository
    ) {
        return TransaccionalProxy.envolver(CrearReservaUseCase.class,
                new CrearReservaService(herramientaRepository, usuarioRepository, reservaRepository, politicaVencimiento,
                        listaEsperaRepository),
                tx, ESCRITURA);
    }

    @Bean
    public VencerReservasUseCase vencerReservasUseCase(
            ReservaRepositoryPort reservaRepository,
            PagoRepositoryPort pagoRepository,
            PoliticaVencimiento politicaVencimiento,
            AvisoListaEspera avisoListaEspera
    ) {
        return TransaccionalProxy.envolver(VencerReservasUseCase.class,
                new VencerReservasService(reservaRepository, pagoRepository, politicaVencimiento, avisoListaEspera), tx, ESCRITURA);
    }

    @Bean
    public GestionarCategoriaUseCase gestionarCategoriaUseCase(
            CategoriaRepositoryPort categoriaRepository,
            HerramientaRepositoryPort herramientaRepository
    ) {
        return TransaccionalProxy.envolver(GestionarCategoriaUseCase.class,
                new GestionarCategoriaService(categoriaRepository, herramientaRepository), tx, ESCRITURA);
    }

    @Bean
    public ConsultarPanelUseCase consultarPanelUseCase(
            UsuarioRepositoryPort usuarioRepository,
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository,
            PagoRepositoryPort pagoRepository,
            FacturaRepositoryPort facturaRepository,
            MantenimientoRepositoryPort mantenimientoRepository
    ) {
        return TransaccionalProxy.envolver(ConsultarPanelUseCase.class,
                new ConsultarPanelService(usuarioRepository, herramientaRepository, reservaRepository,
                        pagoRepository, facturaRepository, mantenimientoRepository),
                tx, LECTURA);
    }

    @Bean
    public GestionarReservaUseCase gestionarReservaUseCase(
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            MantenimientoRepositoryPort mantenimientoRepository,
            ControlAcceso controlAcceso,
            PoliticaVencimiento politicaVencimiento,
            FacturaRepositoryPort facturaRepository,
            @Value("${app.reservas.recargo-mora}") BigDecimal recargoMora,
            AvisoListaEspera avisoListaEspera
    ) {
        return TransaccionalProxy.envolver(GestionarReservaUseCase.class,
                new GestionarReservaService(reservaRepository, herramientaRepository, mantenimientoRepository, controlAcceso,
                        politicaVencimiento, facturaRepository, recargoMora, avisoListaEspera),
                tx, ESCRITURA);
    }

    @Bean
    public ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase(
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository
    ) {
        return TransaccionalProxy.envolver(ConsultarDisponibilidadUseCase.class,
                new ConsultarDisponibilidadService(herramientaRepository, reservaRepository), tx, LECTURA);
    }

    @Bean
    public ProcesarPagoUseCase procesarPagoUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            FacturaRepositoryPort facturaRepository,
            PoliticaVencimiento politicaVencimiento
    ) {
        return TransaccionalProxy.envolver(ProcesarPagoUseCase.class,
                new ProcesarPagoService(pagoRepository, reservaRepository, herramientaRepository, usuarioRepository,
                        facturaRepository, politicaVencimiento),
                tx, ESCRITURA);
    }

    @Bean
    public GestionarPagoUseCase gestionarPagoUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            FacturaRepositoryPort facturaRepository,
            AvisoListaEspera avisoListaEspera
    ) {
        return TransaccionalProxy.envolver(GestionarPagoUseCase.class,
                new GestionarPagoService(pagoRepository, reservaRepository, facturaRepository, avisoListaEspera), tx, ESCRITURA);
    }

    @Bean
    public GestionarFacturaUseCase gestionarFacturaUseCase(FacturaRepositoryPort facturaRepository, ControlAcceso controlAcceso) {
        return TransaccionalProxy.envolver(GestionarFacturaUseCase.class,
                new GestionarFacturaService(facturaRepository, controlAcceso), tx, LECTURA);
    }

    @Bean
    public ConsultarReportesUseCase consultarReportesUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            MantenimientoRepositoryPort mantenimientoRepository
    ) {
        return TransaccionalProxy.envolver(ConsultarReportesUseCase.class,
                new ConsultarReportesService(pagoRepository, reservaRepository, herramientaRepository, usuarioRepository,
                        mantenimientoRepository),
                tx, LECTURA);
    }

    @Bean
    public ListarPagosUseCase listarPagosUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            ControlAcceso controlAcceso
    ) {
        return TransaccionalProxy.envolver(ListarPagosUseCase.class,
                new ListarPagosService(pagoRepository, reservaRepository, controlAcceso), tx, LECTURA);
    }

    @Bean
    public GestionarMantenimientoUseCase gestionarMantenimientoUseCase(
            MantenimientoRepositoryPort mantenimientoRepository,
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository,
            ControlAcceso controlAcceso,
            FacturaRepositoryPort facturaRepository
    ) {
        return TransaccionalProxy.envolver(GestionarMantenimientoUseCase.class,
                new GestionarMantenimientoService(mantenimientoRepository, herramientaRepository, reservaRepository, controlAcceso,
                        facturaRepository),
                tx, ESCRITURA);
    }
}
