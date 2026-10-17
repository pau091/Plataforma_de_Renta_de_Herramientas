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

@Configuration
public class BeanConfig {

    @Bean
    public ControlAcceso controlAcceso(HerramientaRepositoryPort herramientaRepository) {
        return new ControlAcceso(herramientaRepository);
    }

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            PasswordHasherPort passwordHasherPort
    ) {
        return new RegistrarUsuarioService(usuarioRepository, passwordHasherPort);
    }

    @Bean
    public AutenticarUsuarioUseCase autenticarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            PasswordHasherPort passwordHasherPort
    ) {
        return new AutenticarUsuarioService(usuarioRepository, passwordHasherPort);
    }

    @Bean
    public GestionarUsuarioUseCase gestionarUsuarioUseCase(
            UsuarioRepositoryPort usuarioRepository,
            PasswordHasherPort passwordHasherPort
    ) {
        return new GestionarUsuarioService(usuarioRepository, passwordHasherPort);
    }

    @Bean
    public GestionarHerramientaUseCase gestionarHerramientaUseCase(
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            CategoriaRepositoryPort categoriaRepository,
            ControlAcceso controlAcceso
    ) {
        return new GestionarHerramientaService(herramientaRepository, usuarioRepository, categoriaRepository, controlAcceso);
    }

    @Bean
    public CrearReservaUseCase crearReservaUseCase(
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            ReservaRepositoryPort reservaRepository
    ) {
        return new CrearReservaService(herramientaRepository, usuarioRepository, reservaRepository);
    }

    @Bean
    public GestionarReservaUseCase gestionarReservaUseCase(
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            MantenimientoRepositoryPort mantenimientoRepository,
            ControlAcceso controlAcceso
    ) {
        return new GestionarReservaService(reservaRepository, herramientaRepository, mantenimientoRepository, controlAcceso);
    }

    @Bean
    public ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase(
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository
    ) {
        return new ConsultarDisponibilidadService(herramientaRepository, reservaRepository);
    }

    @Bean
    public ProcesarPagoUseCase procesarPagoUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository,
            FacturaRepositoryPort facturaRepository
    ) {
        return new ProcesarPagoService(pagoRepository, reservaRepository, herramientaRepository, usuarioRepository, facturaRepository);
    }

    @Bean
    public GestionarFacturaUseCase gestionarFacturaUseCase(FacturaRepositoryPort facturaRepository, ControlAcceso controlAcceso) {
        return new GestionarFacturaService(facturaRepository, controlAcceso);
    }

    @Bean
    public ConsultarReportesUseCase consultarReportesUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            HerramientaRepositoryPort herramientaRepository,
            UsuarioRepositoryPort usuarioRepository
    ) {
        return new ConsultarReportesService(pagoRepository, reservaRepository, herramientaRepository, usuarioRepository);
    }

    @Bean
    public ListarPagosUseCase listarPagosUseCase(
            PagoRepositoryPort pagoRepository,
            ReservaRepositoryPort reservaRepository,
            ControlAcceso controlAcceso
    ) {
        return new ListarPagosService(pagoRepository, reservaRepository, controlAcceso);
    }

    @Bean
    public GestionarMantenimientoUseCase gestionarMantenimientoUseCase(
            MantenimientoRepositoryPort mantenimientoRepository,
            HerramientaRepositoryPort herramientaRepository,
            ReservaRepositoryPort reservaRepository,
            ControlAcceso controlAcceso
    ) {
        return new GestionarMantenimientoService(mantenimientoRepository, herramientaRepository, reservaRepository, controlAcceso);
    }
}
