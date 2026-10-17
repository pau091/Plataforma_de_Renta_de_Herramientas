package com.example.tomcat.config;

import com.example.tomcat.port.in.AutenticarUsuarioUseCase;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.port.in.ConsultarReportesUseCase;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.port.out.PasswordHasherPort;
import com.example.tomcat.repository.CategoriaRepositoryPort;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.service.AutenticarUsuarioService;
import com.example.tomcat.service.ConsultarDisponibilidadService;
import com.example.tomcat.service.ConsultarReportesService;
import com.example.tomcat.service.ControlAcceso;
import com.example.tomcat.service.CrearReservaService;
import com.example.tomcat.service.GestionarFacturaService;
import com.example.tomcat.service.GestionarHerramientaService;
import com.example.tomcat.service.GestionarMantenimientoService;
import com.example.tomcat.service.GestionarReservaService;
import com.example.tomcat.service.GestionarUsuarioService;
import com.example.tomcat.service.ListarPagosService;
import com.example.tomcat.service.ProcesarPagoService;
import com.example.tomcat.service.RegistrarUsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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

    @Bean
    public ControlAcceso controlAcceso(HerramientaRepositoryPort herramientaRepository) {
        return new ControlAcceso(herramientaRepository);
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
            PasswordHasherPort passwordHasherPort
    ) {
        return TransaccionalProxy.envolver(AutenticarUsuarioUseCase.class,
                new AutenticarUsuarioService(usuarioRepository, passwordHasherPort), tx, LECTURA);
    }

    @Bean
    public GestionarUsuarioUseCase gestionarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository,
            PasswordHasherPort passwordHasherPort
    ) {
        return TransaccionalProxy.envolver(GestionarUsuarioUseCase.class,
                new GestionarUsuarioService(usuarioRepository, herramientaRepository, reservaRepository, passwordHasherPort),
                tx, ESCRITURA);
    }

    @Bean
    public GestionarHerramientaUseCase gestionarHerramientaUseCase(
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            CategoriaRepositoryPort categoriaRepository,
            ReservaRepositoryPort reservaRepository,
            MantenimientoRepositoryPort mantenimientoRepository,
            ControlAcceso controlAcceso
    ) {
        return TransaccionalProxy.envolver(GestionarHerramientaUseCase.class,
                new GestionarHerramientaService(herramientaRepository, usuarioRepository, categoriaRepository,
                        reservaRepository, mantenimientoRepository, controlAcceso),
                tx, ESCRITURA);
    }

    @Bean
    public CrearReservaUseCase crearReservaUseCase(
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            ReservaRepositoryPort reservaRepository
    ) {
        return TransaccionalProxy.envolver(CrearReservaUseCase.class,
                new CrearReservaService(herramientaRepository, usuarioRepository, reservaRepository), tx, ESCRITURA);
    }

    @Bean
    public GestionarReservaUseCase gestionarReservaUseCase(
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            MantenimientoRepositoryPort mantenimientoRepository,
            ControlAcceso controlAcceso
    ) {
        return TransaccionalProxy.envolver(GestionarReservaUseCase.class,
                new GestionarReservaService(reservaRepository, herramientaRepository, mantenimientoRepository, controlAcceso),
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
            FacturaRepositoryPort facturaRepository
    ) {
        return TransaccionalProxy.envolver(ProcesarPagoUseCase.class,
                new ProcesarPagoService(pagoRepository, reservaRepository, herramientaRepository, usuarioRepository, facturaRepository),
                tx, ESCRITURA);
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
            UsuarioRepositoryPort usuarioRepository
    ) {
        return TransaccionalProxy.envolver(ConsultarReportesUseCase.class,
                new ConsultarReportesService(pagoRepository, reservaRepository, herramientaRepository, usuarioRepository),
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
            ControlAcceso controlAcceso
    ) {
        return TransaccionalProxy.envolver(GestionarMantenimientoUseCase.class,
                new GestionarMantenimientoService(mantenimientoRepository, herramientaRepository, reservaRepository, controlAcceso),
                tx, ESCRITURA);
    }
}
